package com.securetravels.crm.accounts;

import com.securetravels.crm.accounts.dto.ForecastResponse;
import com.securetravels.crm.accounts.dto.OpportunityCloseRequest;
import com.securetravels.crm.accounts.dto.OpportunityCreateRequest;
import com.securetravels.crm.accounts.dto.OpportunityMoveRequest;
import com.securetravels.crm.accounts.dto.OpportunityResponse;
import com.securetravels.crm.common.audit.AuditAction;
import com.securetravels.crm.common.audit.AuditService;
import com.securetravels.crm.common.exception.BadRequestException;
import com.securetravels.crm.common.exception.ConflictException;
import com.securetravels.crm.common.exception.ForbiddenException;
import com.securetravels.crm.common.exception.NotFoundException;
import com.securetravels.crm.common.util.XssSanitizer;
import com.securetravels.crm.lead.Lead;
import com.securetravels.crm.lead.LeadRepository;
import com.securetravels.crm.user.Role;
import com.securetravels.crm.user.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Phase 7 Module 3 — opportunities and the revenue forecast. Ownership mirrors
 * {@code LeadService}: MANAGER/ADMIN/CEO see all; SALES/OPS are confined to
 * their own rows (opportunity owner is snapshotted from the lead at create).
 * The forecast is computed live from the current stage probabilities over the
 * half-open {@code [from, to)} window on {@code expected_date} — no rollup
 * materialisation until Module 5 shows it is needed.
 */
@Service
public class OpportunityService {

    private static final Set<Role> SEES_ALL = EnumSet.of(Role.MANAGER, Role.ADMIN, Role.CEO);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final OpportunityRepository opportunities;
    private final PipelineStageRepository stages;
    private final LeadRepository leads;
    private final AuditService auditService;

    public OpportunityService(OpportunityRepository opportunities, PipelineStageRepository stages,
                              LeadRepository leads, AuditService auditService) {
        this.opportunities = opportunities;
        this.stages = stages;
        this.leads = leads;
        this.auditService = auditService;
    }

    @Transactional
    public OpportunityResponse create(OpportunityCreateRequest request, UserPrincipal caller) {
        requireCreateRole(caller.role());

        Lead lead = leads.findById(request.leadId())
                .orElseThrow(() -> new NotFoundException("Lead not found: " + request.leadId()));
        if (lead.getStatus() == Lead.Status.LOST) {
            throw new ConflictException("Cannot create an opportunity for a lost lead");
        }
        requireOwnership(lead.getOwnerId(), caller, "create an opportunity for this lead");
        if (opportunities.existsByLeadId(lead.getId())) {
            throw new ConflictException("A lead can have only one opportunity");
        }

        PipelineStage stage = resolveStage(request.stageKey());

        Opportunity opp = new Opportunity();
        opp.setLeadId(lead.getId());
        opp.setAccountId(lead.getAccountId());
        opp.setOwnerId(lead.getOwnerId());
        opp.setStageId(stage.getId());
        opp.setExpectedValue(request.expectedValue());
        opp.setExpectedDate(request.expectedDate());
        opp.setStageMovedAt(Instant.now());

        Opportunity saved = opportunities.save(opp);
        auditService.record("OPPORTUNITY", saved.getId(), AuditAction.CREATE,
                "lead", null, saved.getLeadId().toString());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> list(UUID leadId, UUID ownerId, String stageKey,
                                          Opportunity.Status status, LocalDate from, LocalDate to,
                                          UserPrincipal caller) {
        UUID effectiveOwner = SEES_ALL.contains(caller.role()) ? ownerId : caller.id();
        return opportunities.search(leadId, effectiveOwner, stageKey,
                        status == null ? null : status.name(), from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OpportunityResponse get(UUID id, UserPrincipal caller) {
        Opportunity opp = requireOpportunity(id);
        requireAccess(opp, caller);
        return toResponse(opp);
    }

    @Transactional
    public OpportunityResponse moveStage(UUID id, OpportunityMoveRequest request, UserPrincipal caller) {
        Opportunity opp = requireOpportunity(id);
        requireAccess(opp, caller);
        if (opp.getStatus() != Opportunity.Status.OPEN) {
            throw new ConflictException("Only an OPEN opportunity can be moved (currently " + opp.getStatus() + ")");
        }

        PipelineStage stage = resolveStage(request.stageKey());
        String previousKey = requireStage(opp.getStageId()).getStageKey();
        opp.setStageId(stage.getId());
        opp.setStageMovedAt(Instant.now());

        auditService.statusChange("OPPORTUNITY", opp.getId(), "stage", previousKey, stage.getStageKey());
        return toResponse(opportunities.save(opp));
    }

    @Transactional
    public OpportunityResponse close(UUID id, OpportunityCloseRequest request, UserPrincipal caller) {
        Opportunity opp = requireOpportunity(id);
        requireAccess(opp, caller);
        if (opp.getStatus() != Opportunity.Status.OPEN) {
            throw new ConflictException("Opportunity is already " + opp.getStatus());
        }

        opp.setStatus(request.outcome());
        opp.setClosedAt(Instant.now());
        opp.setClosedBy(caller.id());
        opp.setClosingNote(XssSanitizer.text(request.note()));

        auditService.statusChange("OPPORTUNITY", opp.getId(), "status", "OPEN", request.outcome().name());
        return toResponse(opportunities.save(opp));
    }

    @Transactional(readOnly = true)
    public ForecastResponse forecast(LocalDate from, LocalDate to, UserPrincipal caller) {
        if (!from.isBefore(to)) {
            throw new BadRequestException("Forecast window must satisfy from < to");
        }
        UUID effectiveOwner = SEES_ALL.contains(caller.role()) ? null : caller.id();
        List<Opportunity> rows = opportunities.search(null, effectiveOwner, null, null, from, to);

        Map<UUID, List<Opportunity>> openByStage = new LinkedHashMap<>();
        Map<YearMonth, MutableMonth> byMonth = new LinkedHashMap<>();
        BigDecimal expected = low(0);
        BigDecimal best = low(0);
        BigDecimal won = low(0);
        long openCount = 0;

        for (Opportunity opp : rows) {
            BigDecimal value = opp.getExpectedValue();
            YearMonth month = YearMonth.from(opp.getExpectedDate());
            if (opp.getStatus() == Opportunity.Status.WON) {
                won = won.add(value);
                byMonth.computeIfAbsent(month, m -> new MutableMonth()).won = byMonth.get(month).won.add(value);
            } else if (opp.getStatus() == Opportunity.Status.OPEN) {
                openCount++;
                expected = expected.add(weighted(value, requireStage(opp.getStageId()).getProbabilityWeight()));
                best = best.add(value);
                openByStage.computeIfAbsent(opp.getStageId(), s -> new java.util.ArrayList<>()).add(opp);
                MutableMonth bucket = byMonth.computeIfAbsent(month, m -> new MutableMonth());
                bucket.expected = bucket.expected.add(weighted(value, requireStage(opp.getStageId()).getProbabilityWeight()));
                bucket.best = bucket.best.add(value);
            }
        }

        List<ForecastResponse.StageBucket> stageBuckets = openByStage.entrySet().stream()
                .map(e -> {
                    PipelineStage stage = requireStage(e.getKey());
                    BigDecimal stageExpected = e.getValue().stream()
                            .map(o -> weighted(o.getExpectedValue(), stage.getProbabilityWeight()))
                            .reduce(low(0), BigDecimal::add);
                    BigDecimal stageBest = e.getValue().stream()
                            .map(Opportunity::getExpectedValue)
                            .reduce(low(0), BigDecimal::add);
                    return new ForecastResponse.StageBucket(stage.getStageKey(), stage.getLabel(),
                            stage.getProbabilityWeight(), e.getValue().size(), stageExpected, stageBest);
                })
                .sorted(Comparator.comparing(ForecastResponse.StageBucket::stageKey))
                .toList();

        List<ForecastResponse.MonthBucket> monthBuckets = byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ForecastResponse.MonthBucket(e.getKey().format(MONTH),
                        money(e.getValue().expected), money(e.getValue().best), money(e.getValue().won)))
                .toList();

        return new ForecastResponse(from, to, money(expected), money(best), money(won),
                openCount, stageBuckets, monthBuckets);
    }

    // ------------------------------------------------------------------ helpers

    private Opportunity requireOpportunity(UUID id) {
        return opportunities.findById(id)
                .orElseThrow(() -> new NotFoundException("Opportunity not found: " + id));
    }

    private PipelineStage requireStage(UUID id) {
        return stages.findById(id)
                .orElseThrow(() -> new NotFoundException("Pipeline stage not found: " + id));
    }

    private PipelineStage resolveStage(String stageKey) {
        if (stageKey == null || stageKey.isBlank()) {
            return stages.findByActiveTrueOrderBySortOrderAsc().stream().findFirst()
                    .orElseThrow(() -> new ConflictException("No active pipeline stage exists"));
        }
        return stages.findByStageKeyAndActiveTrue(stageKey)
                .orElseThrow(() -> stages.existsByStageKey(stageKey)
                        ? new ConflictException("Pipeline stage is inactive: " + stageKey)
                        : new NotFoundException("Pipeline stage not found: " + stageKey));
    }

    private void requireAccess(Opportunity opp, UserPrincipal caller) {
        if (!SEES_ALL.contains(caller.role()) && !caller.id().equals(opp.getOwnerId())) {
            throw new ForbiddenException("Only the opportunity owner or a manager can access this opportunity");
        }
    }

    private static void requireOwnership(UUID ownerId, UserPrincipal caller, String action) {
        if (!SEES_ALL.contains(caller.role()) && !caller.id().equals(ownerId)) {
            throw new ForbiddenException("Only the lead owner or a manager can " + action);
        }
    }

    private static void requireCreateRole(Role actual) {
        if (!Set.of(Role.SALES, Role.MANAGER, Role.ADMIN, Role.CEO).contains(actual)) {
            throw new ForbiddenException("Sales users and above can create opportunities");
        }
    }

    private static BigDecimal weighted(BigDecimal value, BigDecimal percent) {
        return value.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal low(int v) { return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP); }

    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }

    private OpportunityResponse toResponse(Opportunity o) {
        PipelineStage stage = requireStage(o.getStageId());
        return new OpportunityResponse(o.getId(), o.getLeadId(), o.getAccountId(),
                stage.getStageKey(), stage.getLabel(), stage.getProbabilityWeight(),
                o.getOwnerId(), o.getExpectedValue(), o.getExpectedDate(), o.getStatus().name(),
                o.getStageMovedAt(), o.getClosedAt(), o.getClosingNote(), o.getClosedBy(),
                o.getCreatedAt(), o.getUpdatedAt());
    }

    /** Mutable accumulator for monthly buckets. */
    private static final class MutableMonth {
        BigDecimal expected = low(0);
        BigDecimal best = low(0);
        BigDecimal won = low(0);
    }
}
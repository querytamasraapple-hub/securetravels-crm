package com.securetravels.crm.commission;

import com.securetravels.crm.accounts.Account;
import com.securetravels.crm.accounts.AccountCommissionPayableRepository;
import com.securetravels.crm.accounts.AccountRepository;
import com.securetravels.crm.booking.Booking;
import com.securetravels.crm.commission.dto.CommissionPlanAssignmentResponse;
import com.securetravels.crm.commission.dto.CommissionPlanAssignRequest;
import com.securetravels.crm.commission.dto.CommissionPlanBasis;
import com.securetravels.crm.commission.dto.CommissionPlanCreateRequest;
import com.securetravels.crm.commission.dto.CommissionPlanMethod;
import com.securetravels.crm.commission.dto.CommissionPlanResponse;
import com.securetravels.crm.commission.dto.CommissionPlanUpdateRequest;
import com.securetravels.crm.commission.dto.CommissionTierRequest;
import com.securetravels.crm.common.audit.AuditAction;
import com.securetravels.crm.common.audit.AuditService;
import com.securetravels.crm.common.config.AppProperties;
import com.securetravels.crm.common.exception.BadRequestException;
import com.securetravels.crm.common.exception.ConflictException;
import com.securetravels.crm.common.exception.ForbiddenException;
import com.securetravels.crm.common.exception.NotFoundException;
import com.securetravels.crm.common.util.XssSanitizer;
import com.securetravels.crm.user.Role;
import com.securetravels.crm.user.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Phase 7 Module 4 — partner commission plans and their assignment to accounts.
 *
 * <p>Reads and writes are both manager-and-up: plan terms are commercial
 * prices paid to a partner, so unlike pipeline stages (which every user reads
 * to build a forecast) they are not part of the sales-facing surface.
 *
 * <p>The plan a booking used is resolved at confirmation time and recorded on
 * the payable, so editing a plan never rewrites an amount already owed. An
 * account with no assigned plan keeps the flat default rate from
 * {@link AppProperties.Commission} — the Module 1 behaviour — which means
 * rolling plans out one account at a time cannot accidentally zero anybody's
 * commission.
 */
@Service
public class CommissionPlanService {

    private static final Logger log = LoggerFactory.getLogger(CommissionPlanService.class);
    private static final Set<Role> MANAGER_AND_UP = EnumSet.of(Role.MANAGER, Role.ADMIN, Role.CEO);

    private final CommissionPlanRepository plans;
    private final CommissionTierRepository tiers;
    private final AccountCommissionPlanRepository assignments;
    private final AccountRepository accounts;
    private final AccountCommissionPayableRepository payables;
    private final AuditService auditService;
    private final AppProperties appProperties;

    public CommissionPlanService(CommissionPlanRepository plans,
                                 CommissionTierRepository tiers,
                                 AccountCommissionPlanRepository assignments,
                                 AccountRepository accounts,
                                 AccountCommissionPayableRepository payables,
                                 AuditService auditService,
                                 AppProperties appProperties) {
        this.plans = plans;
        this.tiers = tiers;
        this.assignments = assignments;
        this.accounts = accounts;
        this.payables = payables;
        this.auditService = auditService;
        this.appProperties = appProperties;
    }

    // ------------------------------------------------------------------ admin CRUD

    @Transactional
    public CommissionPlanResponse create(CommissionPlanCreateRequest request, UserPrincipal caller) {
        requireManager(caller.role());

        if (plans.existsByPlanKey(request.key())) {
            throw new ConflictException("A commission plan with key " + request.key() + " already exists");
        }

        CommissionPlan plan = new CommissionPlan();
        plan.setPlanKey(request.key());
        plan.setLabel(XssSanitizer.text(request.label()));
        plan.setBasis(CommissionPlan.Basis.valueOf(request.basis().name()));
        plan.setMethod(CommissionPlan.Method.valueOf(request.method().name()));
        plan.setRatePercent(request.ratePercent());
        plan.setFixedAmount(request.fixedAmount());
        plan.setMinSalesThreshold(nz(request.minSalesThreshold()));
        plan.setNotes(XssSanitizer.text(request.notes()));
        validateTerms(plan, request.tiers());

        CommissionPlan saved = plans.save(plan);
        replaceTiers(saved, request.tiers());

        auditService.record("COMMISSION_PLAN", saved.getId(), AuditAction.CREATE,
                "key", null, saved.getPlanKey());
        return toResponse(saved, tiers.findByPlanIdOrderByFromAmountAsc(saved.getId()));
    }

    @Transactional
    public CommissionPlanResponse update(UUID id, CommissionPlanUpdateRequest request, UserPrincipal caller) {
        requireManager(caller.role());
        CommissionPlan plan = requirePlan(id);

        if (request.key() != null && !request.key().equals(plan.getPlanKey())) {
            // Payables, assignments and reports reference the plan by key; a
            // rename would make historical rows describe different terms.
            throw new BadRequestException("plan key is immutable after creation");
        }
        if (request.method() != null
                && CommissionPlan.Method.valueOf(request.method().name()) != plan.getMethod()
                && isInUse(id)) {
            throw new ConflictException("Cannot change the method of a plan that has payables or assignments");
        }
        if (request.active() != null && !request.active() && plan.isActive() && hasActiveAssignment(id)) {
            throw new ConflictException("Cannot deactivate a plan that is assigned to an account");
        }

        CommissionPlan.Method method = request.method() == null
                ? plan.getMethod()
                : CommissionPlan.Method.valueOf(request.method().name());
        CommissionPlan.Basis basis = request.basis() == null
                ? plan.getBasis()
                : CommissionPlan.Basis.valueOf(request.basis().name());

        BigDecimal rate = request.ratePercent() == null ? plan.getRatePercent() : request.ratePercent();
        BigDecimal fixed = request.fixedAmount() == null ? plan.getFixedAmount() : request.fixedAmount();

        CommissionPlan.Basis oldBasis = plan.getBasis();
        CommissionPlan.Method oldMethod = plan.getMethod();
        BigDecimal oldRate = plan.getRatePercent();
        BigDecimal oldFixed = plan.getFixedAmount();
        BigDecimal oldThreshold = plan.getMinSalesThreshold();

        if (request.label() != null) plan.setLabel(XssSanitizer.text(request.label()));
        if (request.notes() != null) plan.setNotes(XssSanitizer.text(request.notes()));
        if (request.minSalesThreshold() != null) plan.setMinSalesThreshold(request.minSalesThreshold());
        if (request.active() != null) plan.setActive(request.active());
        plan.setBasis(basis);
        plan.setMethod(method);
        plan.setRatePercent(method == CommissionPlan.Method.PERCENT ? rate : null);
        plan.setFixedAmount(method == CommissionPlan.Method.FIXED ? fixed : null);

        List<CommissionTierRequest> tierInputs = request.tiers() != null
                ? request.tiers()
                : tierRequestsFrom(tiers.findByPlanIdOrderByFromAmountAsc(plan.getId()));
        validateTerms(plan, tierInputs);

        plans.save(plan);
        if (request.tiers() != null) {
            replaceTiers(plan, request.tiers());
        }

        if (oldBasis != basis) {
            auditService.record("COMMISSION_PLAN", plan.getId(), AuditAction.UPDATE,
                    "basis", oldBasis.name(), basis.name());
        }
        if (oldMethod != method) {
            auditService.record("COMMISSION_PLAN", plan.getId(), AuditAction.UPDATE,
                    "method", oldMethod.name(), method.name());
        }
        if (rateChanged(oldRate, plan.getRatePercent())) {
            auditService.record("COMMISSION_PLAN", plan.getId(), AuditAction.UPDATE,
                    "rate_percent", str(oldRate), str(plan.getRatePercent()));
        }
        if (rateChanged(oldFixed, plan.getFixedAmount())) {
            auditService.record("COMMISSION_PLAN", plan.getId(), AuditAction.UPDATE,
                    "fixed_amount", str(oldFixed), str(plan.getFixedAmount()));
        }
        if (rateChanged(oldThreshold, plan.getMinSalesThreshold())) {
            auditService.record("COMMISSION_PLAN", plan.getId(), AuditAction.UPDATE,
                    "min_sales_threshold", str(oldThreshold), str(plan.getMinSalesThreshold()));
        }
        return toResponse(plan, tiers.findByPlanIdOrderByFromAmountAsc(plan.getId()));
    }

    @Transactional
    public void delete(UUID id, UserPrincipal caller) {
        requireManager(caller.role());
        CommissionPlan plan = requirePlan(id);
        if (isInUse(id)) {
            throw new ConflictException("Cannot delete a plan that has payables or assignments");
        }
        auditService.record("COMMISSION_PLAN", plan.getId(), AuditAction.DELETE,
                "key", plan.getPlanKey(), null);
        plans.delete(plan);
    }

    @Transactional(readOnly = true)
    public List<CommissionPlanResponse> list(boolean includeInactive, UserPrincipal caller) {
        requireManager(caller.role());
        List<CommissionPlan> rows = includeInactive
                ? plans.findAllByOrderByPlanKeyAsc()
                : plans.findByActiveTrueOrderByPlanKeyAsc();
        return rows.stream()
                .map(p -> toResponse(p, tiers.findByPlanIdOrderByFromAmountAsc(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CommissionPlanResponse get(UUID id, UserPrincipal caller) {
        requireManager(caller.role());
        CommissionPlan plan = requirePlan(id);
        return toResponse(plan, tiers.findByPlanIdOrderByFromAmountAsc(plan.getId()));
    }

    // ------------------------------------------------------------------ assignment

    @Transactional
    public CommissionPlanAssignmentResponse assign(UUID accountId, CommissionPlanAssignRequest request,
                                                   UserPrincipal caller) {
        requireManager(caller.role());
        Account account = requireCommissionableAccount(accountId);
        CommissionPlan plan = requirePlan(request.planId());
        if (!plan.isActive()) {
            throw new ConflictException("Cannot assign an inactive plan: " + plan.getPlanKey());
        }

        Optional<AccountCommissionPlan> current = assignments.findByAccountIdAndActiveTrue(accountId);
        if (current.isPresent() && current.get().getPlanId().equals(plan.getId())) {
            // Already on this plan: idempotent, and no duplicate history row.
            return toAssignment(current.get(), plan);
        }
        // Supersede rather than update: the previous row records which terms
        // an earlier booking was confirmed under. The deactivation must be
        // flushed before the insert, otherwise Hibernate issues the INSERT
        // first and the partial unique index (one active row per account)
        // rejects it.
        current.ifPresent(existing -> {
            existing.setActive(false);
            assignments.saveAndFlush(existing);
            auditService.record("COMMISSION_PLAN", plan.getId(), AuditAction.UPDATE,
                    "account_unassigned", existing.getAccountId().toString(), null);
        });

        AccountCommissionPlan row = new AccountCommissionPlan();
        row.setAccountId(account.getId());
        row.setPlanId(plan.getId());
        row.setAssignedAt(Instant.now());
        row.setAssignedBy(caller.id());
        AccountCommissionPlan saved = assignments.save(row);

        auditService.record("COMMISSION_PLAN", plan.getId(), AuditAction.UPDATE,
                "account_assigned", null, account.getId().toString());
        return toAssignment(saved, plan);
    }

    @Transactional
    public void unassign(UUID accountId, UserPrincipal caller) {
        requireManager(caller.role());
        AccountCommissionPlan existing = assignments.findByAccountIdAndActiveTrue(accountId)
                .orElseThrow(() -> new NotFoundException("No active commission plan assigned to account " + accountId));
        existing.setActive(false);
        assignments.save(existing);
        auditService.record("COMMISSION_PLAN", existing.getPlanId(), AuditAction.UPDATE,
                "account_unassigned", accountId.toString(), null);
    }

    @Transactional(readOnly = true)
    public CommissionPlanAssignmentResponse assignmentFor(UUID accountId, UserPrincipal caller) {
        requireManager(caller.role());
        AccountCommissionPlan row = assignments.findByAccountIdAndActiveTrue(accountId)
                .orElseThrow(() -> new NotFoundException("No active commission plan assigned to account " + accountId));
        return toAssignment(row, requirePlan(row.getPlanId()));
    }

    // ------------------------------------------------------------------ calculation

    /**
     * What this account's terms say about a booking. Used by the payable credit
     * path and by the quote endpoint; never writes anything itself.
     */
    @Transactional(readOnly = true)
    public CommissionQuote quoteForAccount(UUID accountId, BigDecimal gross, BigDecimal discount, BigDecimal tax) {
        CommissionPlan plan = assignments.findByAccountIdAndActiveTrue(accountId)
                .map(a -> plans.findById(a.getPlanId()).orElse(null))
                .orElse(null);
        if (plan == null || !plan.isActive()) {
            return flatDefaultQuote(gross, discount, tax);
        }
        List<CommissionTier> planTiers = plan.getMethod() == CommissionPlan.Method.TIERED
                ? tiers.findByPlanIdOrderByFromAmountAsc(plan.getId())
                : List.of();
        CommissionQuote quote = CommissionCalculator.quote(plan, gross, discount, tax, planTiers);
        if (quote.appliedRatePercent() == null && plan.getMethod() == CommissionPlan.Method.TIERED) {
            // Only reachable if a tier set was emptied behind the service's back.
            log.warn("[commission] plan {} matched no tier for basis {}; accruing 0",
                    plan.getPlanKey(), quote.basisAmount());
        }
        return quote;
    }

    /** Manager-only preview of the same calculation {@link #quoteForBooking} runs. */
    @Transactional(readOnly = true)
    public CommissionQuote quotePreview(UUID accountId, BigDecimal gross, BigDecimal discount, BigDecimal tax,
                                        UserPrincipal caller) {
        requireManager(caller.role());
        return quoteForAccount(accountId, gross, discount, tax);
    }

    /** Convenience for the confirm path, which already holds the account. */
    public CommissionQuote quoteForBooking(Booking booking) {
        return quoteForAccount(booking.getAccountId(), booking.getTotalAmount(),
                booking.getDiscountAmount(), booking.getTaxAmount());
    }

    // ------------------------------------------------------------------ helpers

    private CommissionQuote flatDefaultQuote(BigDecimal gross, BigDecimal discount, BigDecimal tax) {
        BigDecimal g = nz(gross);
        BigDecimal basis = money(g.subtract(nz(discount)).add(nz(tax)));
        BigDecimal rate = appProperties.getCommission().getDefaultTravelAgentPercent();
        return CommissionQuote.flatRate(rate, basis,
                money(basis.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)),
                CommissionPlan.Basis.NET);
    }

    private void validateTerms(CommissionPlan plan, List<CommissionTierRequest> tierInputs) {
        switch (plan.getMethod()) {
            case PERCENT -> {
                if (plan.getRatePercent() == null) {
                    throw new BadRequestException("A PERCENT plan requires ratePercent");
                }
                requireNoTiers(plan, tierInputs);
            }
            case FIXED -> {
                if (plan.getFixedAmount() == null) {
                    throw new BadRequestException("A FIXED plan requires fixedAmount");
                }
                requireNoTiers(plan, tierInputs);
            }
            case TIERED -> {
                requireTierBands(plan, tierInputs);
                if (plan.getRatePercent() != null || plan.getFixedAmount() != null) {
                    throw new BadRequestException("A TIERED plan takes its rates from its tiers, "
                            + "not from ratePercent/fixedAmount");
                }
            }
        }
    }

    private static void requireNoTiers(CommissionPlan plan, List<CommissionTierRequest> tierInputs) {
        if (tierInputs != null && !tierInputs.isEmpty()) {
            throw new BadRequestException("Tiers are only valid on a TIERED plan (plan " + plan.getPlanKey() + ")");
        }
    }

    /**
     * Bands must tile {@code [0, ∞)} with no gap and no overlap, otherwise a
     * booking value could fall between two bands and be owed nothing at all.
     * Open-ended is only allowed on the last band, so resolution always
     * terminates.
     */
    private static void requireTierBands(CommissionPlan plan, List<CommissionTierRequest> tierInputs) {
        if (tierInputs == null || tierInputs.isEmpty()) {
            throw new BadRequestException("A TIERED plan requires at least one tier");
        }
        List<CommissionTierRequest> sorted = tierInputs.stream()
                .sorted(Comparator.comparing(CommissionTierRequest::fromAmount))
                .toList();

        BigDecimal expectedFrom = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (int i = 0; i < sorted.size(); i++) {
            CommissionTierRequest tier = sorted.get(i);
            BigDecimal from = money(tier.fromAmount());
            BigDecimal to = tier.toAmount() == null ? null : money(tier.toAmount());

            if (from.compareTo(expectedFrom) != 0) {
                throw new BadRequestException("Tiers must be contiguous starting at 0: expected a tier starting at "
                        + expectedFrom.toPlainString() + " but found " + from.toPlainString()
                        + (from.compareTo(expectedFrom) > 0 ? " (gap)" : " (overlap)"));
            }
            if (to == null) {
                if (i != sorted.size() - 1) {
                    throw new BadRequestException("Only the last tier may be open-ended");
                }
                return;
            }
            expectedFrom = to;
        }
        throw new BadRequestException("The top tier must be open-ended (toAmount null) so every booking value "
                + "is covered");
    }

    private void replaceTiers(CommissionPlan plan, List<CommissionTierRequest> tierInputs) {
        tiers.deleteByPlanId(plan.getId());
        if (tierInputs == null || tierInputs.isEmpty()) {
            return;
        }
        List<CommissionTier> rows = new ArrayList<>();
        for (CommissionTierRequest input : tierInputs) {
            rows.add(new CommissionTier(plan, money(input.fromAmount()),
                    input.toAmount() == null ? null : money(input.toAmount()),
                    money(input.ratePercent())));
        }
        tiers.saveAll(rows);
    }

    private static List<CommissionTierRequest> tierRequestsFrom(List<CommissionTier> bands) {
        return bands.stream()
                .map(t -> new CommissionTierRequest(t.getFromAmount(), t.getToAmount(), t.getRatePercent()))
                .toList();
    }

    /**
     * A plan is in use once it has produced a payable or has been assigned to an
     * account. Both make its terms a historical fact rather than a proposal.
     */
    private boolean isInUse(UUID planId) {
        return payables.existsByPlanId(planId) || !assignments.findByPlanId(planId).isEmpty();
    }

    private boolean hasActiveAssignment(UUID planId) {
        return assignments.findByPlanId(planId).stream().anyMatch(AccountCommissionPlan::isActive);
    }

    private Account requireCommissionableAccount(UUID accountId) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        if (account.getAccountType() != Account.AccountType.TRAVEL_AGENT) {
            throw new BadRequestException("Only a TRAVEL_AGENT account can carry a commission plan");
        }
        if (!account.isActive()) {
            throw new ConflictException("Cannot assign a plan to an inactive account");
        }
        return account;
    }

    private CommissionPlan requirePlan(UUID id) {
        return plans.findById(id)
                .orElseThrow(() -> new NotFoundException("Commission plan not found: " + id));
    }

    private static void requireManager(Role actual) {
        if (!MANAGER_AND_UP.contains(actual)) {
            throw new ForbiddenException("Only managers can manage commission plans");
        }
    }

    private static boolean rateChanged(BigDecimal before, BigDecimal after) {
        return nz(before).compareTo(nz(after)) != 0;
    }

    private static String str(BigDecimal value) {
        return nz(value).toPlainString();
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static CommissionPlanResponse toResponse(CommissionPlan p, List<CommissionTier> tiers) {
        List<CommissionPlanResponse.Tier> bandViews = tiers.stream()
                .map(t -> new CommissionPlanResponse.Tier(t.getId(), t.getFromAmount(),
                        t.getToAmount(), t.getRatePercent()))
                .toList();
        return new CommissionPlanResponse(p.getId(), p.getPlanKey(), p.getLabel(),
                CommissionPlanBasis.valueOf(p.getBasis().name()),
                CommissionPlanMethod.valueOf(p.getMethod().name()),
                p.getRatePercent(), p.getFixedAmount(), p.getMinSalesThreshold(),
                p.isActive(), bandViews, p.getNotes());
    }

    private static CommissionPlanAssignmentResponse toAssignment(AccountCommissionPlan a, CommissionPlan p) {
        return new CommissionPlanAssignmentResponse(a.getId(), a.getAccountId(), a.getPlanId(),
                p.getPlanKey(), a.getAssignedAt(), a.getAssignedBy());
    }
}

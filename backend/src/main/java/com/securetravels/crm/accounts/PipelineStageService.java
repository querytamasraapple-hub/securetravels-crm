package com.securetravels.crm.accounts;

import com.securetravels.crm.accounts.dto.PipelineStageCreateRequest;
import com.securetravels.crm.accounts.dto.PipelineStageResponse;
import com.securetravels.crm.accounts.dto.PipelineStageUpdateRequest;
import com.securetravels.crm.common.audit.AuditAction;
import com.securetravels.crm.common.audit.AuditService;
import com.securetravels.crm.common.exception.ConflictException;
import com.securetravels.crm.common.exception.ForbiddenException;
import com.securetravels.crm.common.exception.NotFoundException;
import com.securetravels.crm.common.util.XssSanitizer;
import com.securetravels.crm.user.Role;
import com.securetravels.crm.user.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Phase 7 Module 2 — configurable opportunity pipeline stages. Reads are any
 * authenticated user; writes are manager-and-up (enforced here, mirroring
 * {@link AccountService}). Invariants guarded here and durably in V20:
 * immutable key, unique active sort order (deterministic list order), weights
 * bounded 0–100, and at least one active stage always remaining. The hard
 * delete is safe today because nothing references stages yet; Module 3 turns
 * stage deletion into a deactivation when opportunities reference rows.
 */
@Service
public class PipelineStageService {

    private static final Set<Role> MANAGER_AND_UP = EnumSet.of(Role.MANAGER, Role.ADMIN, Role.CEO);

    private final PipelineStageRepository stages;
    private final AuditService auditService;

    public PipelineStageService(PipelineStageRepository stages, AuditService auditService) {
        this.stages = stages;
        this.auditService = auditService;
    }

    @Transactional
    public PipelineStageResponse create(PipelineStageCreateRequest request, UserPrincipal caller) {
        requireManager(caller.role());

        if (stages.existsByStageKey(request.key())) {
            throw new ConflictException("A pipeline stage with key " + request.key() + " already exists");
        }
        int sortOrder = request.sortOrder();
        if (stages.existsByActiveTrueAndSortOrder(sortOrder)) {
            throw new ConflictException("Another active stage already uses sort order " + sortOrder);
        }

        PipelineStage stage = new PipelineStage();
        stage.setStageKey(request.key());
        stage.setLabel(XssSanitizer.text(request.label()));
        stage.setSortOrder(sortOrder);
        stage.setProbabilityWeight(request.probabilityWeight());
        stage.setEntryCondition(XssSanitizer.text(request.entryCondition()));
        stage.setActive(request.active() == null || request.active());

        PipelineStage saved = stages.save(stage);
        auditService.record("PIPELINE_STAGE", saved.getId(), AuditAction.CREATE,
                "key", null, saved.getStageKey());
        return toResponse(saved);
    }

    @Transactional
    public PipelineStageResponse update(UUID id, PipelineStageUpdateRequest request, UserPrincipal caller) {
        requireManager(caller.role());
        PipelineStage stage = requireStage(id);

        if (request.label() != null) stage.setLabel(XssSanitizer.text(request.label()));
        if (request.probabilityWeight() != null) stage.setProbabilityWeight(request.probabilityWeight());
        if (request.entryCondition() != null) stage.setEntryCondition(XssSanitizer.text(request.entryCondition()));
        if (request.sortOrder() != null) stage.setSortOrder(request.sortOrder());

        boolean wasActive = stage.isActive();
        boolean nowActive = request.active() == null ? wasActive : request.active();
        if (!wasActive && nowActive && stages.existsByActiveTrueAndSortOrderAndIdNot(stage.getSortOrder(), id)) {
            throw new ConflictException("Another active stage already uses sort order " + stage.getSortOrder());
        }
        if (wasActive && !nowActive && stages.countByActiveTrue() <= 1) {
            throw new ConflictException("Cannot deactivate the last active pipeline stage");
        }
        stage.setActive(nowActive);

        auditService.record("PIPELINE_STAGE", stage.getId(), AuditAction.UPDATE,
                "key", null, stage.getStageKey());
        return toResponse(stages.save(stage));
    }

    @Transactional
    public void delete(UUID id, UserPrincipal caller) {
        requireManager(caller.role());
        PipelineStage stage = requireStage(id);
        if (stage.isActive() && stages.countByActiveTrue() <= 1) {
            throw new ConflictException("Cannot delete the last active pipeline stage");
        }
        auditService.record("PIPELINE_STAGE", stage.getId(), AuditAction.DELETE,
                "key", stage.getStageKey(), null);
        stages.delete(stage);
    }

    @Transactional(readOnly = true)
    public List<PipelineStageResponse> list(boolean includeInactive, UserPrincipal caller) {
        if (includeInactive) requireManager(caller.role());
        List<PipelineStage> rows = includeInactive
                ? stages.findAllByOrderBySortOrderAscStageKeyAsc()
                : stages.findByActiveTrueOrderBySortOrderAsc();
        return rows.stream().map(PipelineStageService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PipelineStageResponse get(UUID id, UserPrincipal caller) {
        return toResponse(requireStage(id));
    }

    // ------------------------------------------------------------------ helpers

    private PipelineStage requireStage(UUID id) {
        return stages.findById(id)
                .orElseThrow(() -> new NotFoundException("Pipeline stage not found: " + id));
    }

    private static void requireManager(Role actual) {
        if (!MANAGER_AND_UP.contains(actual)) {
            throw new ForbiddenException("Only managers can manage pipeline stages");
        }
    }

    private static PipelineStageResponse toResponse(PipelineStage s) {
        return new PipelineStageResponse(s.getId(), s.getStageKey(), s.getLabel(), s.getSortOrder(),
                s.getProbabilityWeight(), s.getEntryCondition(),
                s.isActive(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
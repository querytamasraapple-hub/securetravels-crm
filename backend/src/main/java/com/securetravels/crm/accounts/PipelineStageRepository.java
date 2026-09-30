package com.securetravels.crm.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pipeline stage store. The DB enforces unique {@code stage_key} and a
 * partial unique {@code sort_order} among active stages; the service layered
 * on top turns those into user-friendly conflicts.
 */
public interface PipelineStageRepository extends JpaRepository<PipelineStage, UUID> {

    List<PipelineStage> findByActiveTrueOrderBySortOrderAsc();

    List<PipelineStage> findAllByOrderBySortOrderAscStageKeyAsc();

    Optional<PipelineStage> findByStageKey(String stageKey);

    boolean existsByStageKey(String stageKey);

    boolean existsByStageKeyAndIdNot(String stageKey, UUID id);

    boolean existsByActiveTrueAndSortOrder(int sortOrder);

    boolean existsByActiveTrueAndSortOrderAndIdNot(int sortOrder, UUID id);

    long countByActiveTrue();
}
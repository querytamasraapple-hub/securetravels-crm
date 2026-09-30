package com.securetravels.crm.accounts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Opportunity store. The working set for the forecast is read as a single
 * scoped list (small volumes) and aggregated in {@link OpportunityService} —
 * the live-compute pattern, so data never drifts from source. Ownership
 * scope is applied by the service, not here.
 */
public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    boolean existsByLeadId(UUID leadId);

    @Query(value = """
            select o.* from opportunities o
            join pipeline_stages s on s.id = o.stage_id
            where (CAST(:leadId AS uuid) is null or o.lead_id = :leadId)
              and (CAST(:ownerId AS uuid) is null or o.owner_id = :ownerId)
              and (CAST(:stageKey AS varchar) is null or s.stage_key = :stageKey)
              and (CAST(:status AS varchar) is null or o.status = :status)
              and (CAST(:from AS date) is null or o.expected_date >= :from)
              and (CAST(:to AS date) is null or o.expected_date < :to)
            order by o.expected_date, o.created_at
            """,
            nativeQuery = true)
    List<Opportunity> search(@Param("leadId") UUID leadId,
                             @Param("ownerId") UUID ownerId,
                             @Param("stageKey") String stageKey,
                             @Param("status") String status,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to);
}
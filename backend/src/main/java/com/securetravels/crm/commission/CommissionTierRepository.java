package com.securetravels.crm.commission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Tier store for TIERED plans. Bands are always read in ascending
 * {@code fromAmount} order so boundary resolution walks them deterministically.
 */
public interface CommissionTierRepository extends JpaRepository<CommissionTier, UUID> {

    List<CommissionTier> findByPlanIdOrderByFromAmountAsc(UUID planId);

    @Modifying
    @Query("delete from CommissionTier t where t.plan.id = :planId")
    void deleteByPlanId(@Param("planId") UUID planId);
}

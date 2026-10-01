package com.securetravels.crm.commission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Commission plan store. The DB owns the unique {@code plan_key}; the service
 * turns that (and tier/assignment collisions) into user-friendly conflicts.
 * Tier bands live in {@link CommissionTierRepository}.
 */
public interface CommissionPlanRepository extends JpaRepository<CommissionPlan, UUID> {

    Optional<CommissionPlan> findByPlanKey(String planKey);

    List<CommissionPlan> findByActiveTrueOrderByPlanKeyAsc();

    List<CommissionPlan> findAllByOrderByPlanKeyAsc();

    boolean existsByPlanKey(String planKey);
}

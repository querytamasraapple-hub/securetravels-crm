package com.securetravels.crm.commission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Plan-to-account assignment store. The partial unique index
 * {@code idx_account_commission_plans_active} guarantees at most one active
 * row per account; these lookups stay simple because of it.
 */
public interface AccountCommissionPlanRepository extends JpaRepository<AccountCommissionPlan, UUID> {

    Optional<AccountCommissionPlan> findByAccountIdAndActiveTrue(UUID accountId);

    List<AccountCommissionPlan> findByPlanId(UUID planId);
}

package com.securetravels.crm.commission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Phase 7 Module 4 — which plan serves an account. Assignment rows are
 * retained rather than updated: re-assigning deactivates the previous row and
 * inserts a new one, so "which terms were in force when this booking was
 * confirmed" stays answerable.
 *
 * <p>The DB enforces at most one active assignment per account with a partial
 * unique index; {@link CommissionPlanService} turns a second attempt into a
 * clean 409 instead of a constraint violation.
 */
@Entity
@Table(name = "account_commission_plans")
public class AccountCommissionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
    public UUID getAssignedBy() { return assignedBy; }
    public void setAssignedBy(UUID assignedBy) { this.assignedBy = assignedBy; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

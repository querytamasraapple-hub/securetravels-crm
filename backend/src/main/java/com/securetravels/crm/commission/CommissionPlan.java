package com.securetravels.crm.commission;

import com.securetravels.crm.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Phase 7 Module 4 — partner-commission terms for a travel-agent account.
 * A plan says <em>how</em> a commission is computed: the basis it applies
 * to (NET = gross - discount + tax, matching V13's ledger definition, or
 * the undiscounted GROSS), the method, and the minimum booking value below
 * which nothing is owed.
 *
 * <p>Methods are deliberately mutually exclusive and the DB enforces it
 * ({@code chk_commission_plan_method_inputs}): PERCENT needs a rate, FIXED
 * needs an amount, TIERED reads neither because its rates live in
 * {@link CommissionTier}. A plan is a snapshot of intent — the payable
 * written at confirmation records the plan id and the applied rate, so
 * later plan edits never rewrite what was already owed.
 */
@Entity
@Table(name = "commission_plans", indexes = {
        @Index(name = "idx_commission_plans_active", columnList = "is_active")
})
public class CommissionPlan extends Auditable {

    /** What the commission is computed on. */
    public enum Basis { NET, GROSS }

    /** How the commission is derived from that basis. */
    public enum Method { PERCENT, FIXED, TIERED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_key", nullable = false, length = 40, unique = true)
    private String planKey;

    @Column(name = "label", nullable = false, length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "basis", nullable = false, length = 10)
    private Basis basis = Basis.NET;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 10)
    private Method method;

    @Column(name = "rate_percent", precision = 5, scale = 2)
    private BigDecimal ratePercent;

    @Column(name = "fixed_amount", precision = 12, scale = 2)
    private BigDecimal fixedAmount;

    @Column(name = "min_sales_threshold", nullable = false, precision = 12, scale = 2)
    private BigDecimal minSalesThreshold = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "notes")
    private String notes;

    // Tiers are loaded through CommissionTierRepository rather than a
    // collection mapping: the service replaces the whole tier set on update
    // (delete + re-insert, so band ids change and no stale instance lingers in
    // the persistence context).

    public UUID getId() { return id; }
    public String getPlanKey() { return planKey; }
    public void setPlanKey(String planKey) { this.planKey = planKey; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Basis getBasis() { return basis; }
    public void setBasis(Basis basis) { this.basis = basis; }
    public Method getMethod() { return method; }
    public void setMethod(Method method) { this.method = method; }
    public BigDecimal getRatePercent() { return ratePercent; }
    public void setRatePercent(BigDecimal ratePercent) { this.ratePercent = ratePercent; }
    public BigDecimal getFixedAmount() { return fixedAmount; }
    public void setFixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; }
    public BigDecimal getMinSalesThreshold() { return minSalesThreshold; }
    public void setMinSalesThreshold(BigDecimal minSalesThreshold) { this.minSalesThreshold = minSalesThreshold; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

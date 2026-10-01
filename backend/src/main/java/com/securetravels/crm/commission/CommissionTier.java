package com.securetravels.crm.commission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One band of a TIERED commission plan: {@code rate_percent} applies when the
 * commission basis falls in the half-open interval
 * {@code [from_amount, to_amount)}.
 *
 * <p>Half-open is the whole point. Closed intervals would make an amount
 * exactly on a boundary match two tiers, so the computed commission would
 * depend on row order — the kind of bug that only shows up when real money
 * lands exactly on a round number, which is to say eventually. As written, a
 * value equal to {@code from_amount} belongs to this tier and a value equal to
 * {@code to_amount} belongs to the next one up. {@code to_amount IS NULL}
 * means the top band is open-ended.
 *
 * <p>Append-only: tiers carry {@code created_at} but no {@code updated_at} or
 * {@code version}, because a tier is replaced by rewriting the plan's tier set
 * rather than edited in place.
 */
@Entity
@Table(name = "commission_tiers")
public class CommissionTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private CommissionPlan plan;

    @Column(name = "from_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal fromAmount;

    @Column(name = "to_amount", precision = 12, scale = 2)
    private BigDecimal toAmount;

    @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal ratePercent;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public CommissionTier() {
    }

    public CommissionTier(CommissionPlan plan, BigDecimal fromAmount, BigDecimal toAmount,
                          BigDecimal ratePercent) {
        this.plan = plan;
        this.fromAmount = fromAmount;
        this.toAmount = toAmount;
        this.ratePercent = ratePercent;
    }

    /** True when {@code basisAmount} falls in this tier's half-open band. */
    public boolean covers(BigDecimal basisAmount) {
        if (basisAmount.compareTo(fromAmount) < 0) {
            return false;
        }
        return toAmount == null || basisAmount.compareTo(toAmount) < 0;
    }

    public UUID getId() { return id; }
    public CommissionPlan getPlan() { return plan; }
    public void setPlan(CommissionPlan plan) { this.plan = plan; }
    public BigDecimal getFromAmount() { return fromAmount; }
    public void setFromAmount(BigDecimal fromAmount) { this.fromAmount = fromAmount; }
    public BigDecimal getToAmount() { return toAmount; }
    public void setToAmount(BigDecimal toAmount) { this.toAmount = toAmount; }
    public BigDecimal getRatePercent() { return ratePercent; }
    public void setRatePercent(BigDecimal ratePercent) { this.ratePercent = ratePercent; }
    public Instant getCreatedAt() { return createdAt; }
}

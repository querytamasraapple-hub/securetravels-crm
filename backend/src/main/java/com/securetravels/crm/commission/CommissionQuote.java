package com.securetravels.crm.commission;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The result of running a {@link CommissionPlan} against a booking's amounts.
 * Deliberately explicit about which tier and which rate produced the number —
 * a partner disputing an invoice needs "12% from tier 2" in the record, not
 * just an amount to reverse-engineer.
 *
 * @param planId       the plan applied, or null when no plan was assigned and
 *                     the flat default rate was used instead
 * @param planKey      readable key of that plan
 * @param basis        the basis the commission was computed on
 * @param method       the method used
 * @param basisAmount  the amount the method was applied to (scale 2)
 * @param commissionAmount what is owed (scale 2); zero when below the threshold
 * @param appliedRatePercent the rate that produced it: a PERCENT rate, the
 *                     matched TIERED band rate, or null for FIXED plans and for
 *                     a TIERED value that matched no band
 * @param matchedTierFrom / {@code matchedTierTo} the band that matched, or null
 * @param belowThreshold whether the plan's minimum-sales threshold was not met
 * @param minSalesThreshold the threshold that applies, for the audit note
 */
public record CommissionQuote(
        UUID planId,
        String planKey,
        CommissionPlan.Basis basis,
        CommissionPlan.Method method,
        BigDecimal basisAmount,
        BigDecimal commissionAmount,
        BigDecimal appliedRatePercent,
        BigDecimal matchedTierFrom,
        BigDecimal matchedTierTo,
        boolean belowThreshold,
        BigDecimal minSalesThreshold
) {

    static CommissionQuote of(CommissionPlan plan, BigDecimal basisAmount, BigDecimal commissionAmount,
                              BigDecimal appliedRatePercent, CommissionTier tier) {
        return new CommissionQuote(plan.getId(), plan.getPlanKey(), plan.getBasis(), plan.getMethod(),
                basisAmount, commissionAmount, appliedRatePercent,
                tier == null ? null : tier.getFromAmount(),
                tier == null ? null : tier.getToAmount(),
                false, plan.getMinSalesThreshold());
    }

    static CommissionQuote belowThreshold(CommissionPlan plan, BigDecimal basisAmount,
                                          BigDecimal threshold) {
        return new CommissionQuote(plan.getId(), plan.getPlanKey(), plan.getBasis(), plan.getMethod(),
                basisAmount, BigDecimal.ZERO.setScale(2), null, null, null, true, threshold);
    }

    /**
     * The fallback used when an account has no plan: the flat default rate from
     * configuration. This is the Module 1 behaviour, preserved so enabling
     * plans is opt-in per account and no account silently drops to zero
     * commission.
     */
    static CommissionQuote flatRate(BigDecimal defaultRatePercent, BigDecimal basisAmount,
                                    BigDecimal commissionAmount, CommissionPlan.Basis basis) {
        return new CommissionQuote(null, "DEFAULT_FLAT_RATE", basis, CommissionPlan.Method.PERCENT,
                basisAmount, commissionAmount, defaultRatePercent, null, null, false, BigDecimal.ZERO);
    }

    public boolean isOwed() {
        return commissionAmount.signum() > 0;
    }
}

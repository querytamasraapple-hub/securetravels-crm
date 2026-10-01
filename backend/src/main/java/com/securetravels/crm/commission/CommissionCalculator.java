package com.securetravels.crm.commission;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Phase 7 Module 4 — the commission arithmetic, as a pure function of a plan
 * and a booking's amounts.
 *
 * <p>Kept free of Spring, JPA and the database on purpose. Commission is the
 * one calculation in this codebase where an off-by-one is a contractual
 * dispute with a partner, so the boundary behaviour is pinned by unit tests on
 * exact edge amounts rather than only through HTTP-level integration tests.
 *
 * <p>Two rules define the edges:
 * <ul>
 *   <li>Bands are half-open {@code [from, to)} — a value exactly on a boundary
 *       is paid at the <em>upper</em> tier's rate, never at two rates and never
 *       at none.</li>
 *   <li>The minimum-sales threshold is compared against the same basis the
 *       commission is computed on, and is inclusive: a booking exactly at the
 *       threshold is commissionable.</li>
 * </ul>
 */
public final class CommissionCalculator {

    private CommissionCalculator() {
    }

    /**
     * Compute what a plan owes on a booking.
     *
     * @param gross    booking total before discount
     * @param discount booking discount
     * @param tax      booking tax
     * @param tiers    the plan's bands, already in ascending {@code fromAmount} order
     */
    public static CommissionQuote quote(CommissionPlan plan,
                                        BigDecimal gross,
                                        BigDecimal discount,
                                        BigDecimal tax,
                                        List<CommissionTier> tiers) {
        BigDecimal g = nz(gross);
        BigDecimal basisAmount = money(plan.getBasis() == CommissionPlan.Basis.GROSS
                ? g
                : g.subtract(nz(discount)).add(nz(tax)));

        BigDecimal threshold = nz(plan.getMinSalesThreshold());
        if (basisAmount.compareTo(threshold) < 0) {
            return CommissionQuote.belowThreshold(plan, basisAmount, threshold);
        }

        return switch (plan.getMethod()) {
            case PERCENT -> CommissionQuote.of(plan, basisAmount,
                    money(basisAmount.multiply(nz(plan.getRatePercent()))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)),
                    plan.getRatePercent(), null);
            case FIXED -> CommissionQuote.of(plan, basisAmount,
                    money(nz(plan.getFixedAmount())), null, null);
            case TIERED -> tiered(plan, basisAmount, tiers);
        };
    }

    private static CommissionQuote tiered(CommissionPlan plan, BigDecimal basisAmount,
                                           List<CommissionTier> tiers) {
        for (CommissionTier tier : tiers) {
            if (tier.covers(basisAmount)) {
                return CommissionQuote.of(plan, basisAmount,
                        money(basisAmount.multiply(tier.getRatePercent())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)),
                        tier.getRatePercent(), tier);
            }
        }
        // Unreachable for a validated plan: tiers must start at 0 and only the
        // top band may be open-ended. Reported as zero rather than thrown so a
        // booking confirmation can never fail on a commission lookup.
        return CommissionQuote.of(plan, basisAmount, BigDecimal.ZERO.setScale(2), null, null);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}

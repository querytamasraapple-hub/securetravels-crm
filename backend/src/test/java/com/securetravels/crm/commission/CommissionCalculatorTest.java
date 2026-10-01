package com.securetravels.crm.commission;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 7 Module 4 — the non-negotiable tiered-boundary test.
 *
 * <p>Commission is money owed to a partner, so the exact behaviour at a tier
 * edge is a contractual question, not a rounding detail. These cases are
 * pinned on the pure calculator with amounts that land precisely on the
 * boundaries a real booking would hit (round thousands, GST-inclusive totals):
 *
 * <ul>
 *   <li>a value exactly on a boundary belongs to the <b>upper</b> tier —
 *       never both, never neither;</li>
 *   <li>the lowest tier starts at 0 and the top tier is open-ended, so no
 *       amount is ever unpriced;</li>
 *   <li>the minimum-sales threshold is inclusive: a booking exactly at the
 *       threshold is still commissionable.</li>
 * </ul>
 */
class CommissionCalculatorTest {

    /** 0–50,000 at 5%; 50,000–100,000 at 10%; 100,000+ at 15%. */
    private static CommissionPlan tieredPlan(String minSalesThreshold) {
        CommissionPlan plan = new CommissionPlan();
        plan.setPlanKey("TIERED_TEST");
        plan.setLabel("Tiered test plan");
        plan.setBasis(CommissionPlan.Basis.NET);
        plan.setMethod(CommissionPlan.Method.TIERED);
        plan.setMinSalesThreshold(new BigDecimal(minSalesThreshold));
        return plan;
    }

    private static List<CommissionTier> bands() {
        CommissionPlan owner = new CommissionPlan();
        return List.of(
                new CommissionTier(owner, money("0"), money("50000"), money("5")),
                new CommissionTier(owner, money("50000"), money("100000"), money("10")),
                new CommissionTier(owner, money("100000"), null, money("15")));
    }

    @Test
    void amountExactlyOnABoundaryPaysAtTheUpperTier() {
        CommissionPlan plan = tieredPlan("0");
        List<CommissionTier> tiers = bands();

        // 49999.99 is still the bottom tier (5% of it is 2499.9995 -> 2500.00).
        assertThat(CommissionCalculator.quote(plan, money("49999.99"), null, null, tiers)
                .commissionAmount()).isEqualByComparingTo("2500.00");
        // 50000 exactly belongs to the *second* tier: 10% of 50000.
        assertThat(CommissionCalculator.quote(plan, money("50000"), null, null, tiers)
                .commissionAmount()).isEqualByComparingTo("5000.00");
        // 100000 exactly belongs to the third tier: 15% of 100000.
        assertThat(CommissionCalculator.quote(plan, money("100000"), null, null, tiers)
                .commissionAmount()).isEqualByComparingTo("15000.00");
    }

    @Test
    void onePaisaEitherSideOfABoundarySwitchesTier() {
        CommissionPlan plan = tieredPlan("0");
        List<CommissionTier> tiers = bands();

        CommissionQuote below = CommissionCalculator.quote(plan, money("49999.99"), null, null, tiers);
        CommissionQuote at = CommissionCalculator.quote(plan, money("50000"), null, null, tiers);
        CommissionQuote above = CommissionCalculator.quote(plan, money("50000.01"), null, null, tiers);

        assertThat(below.appliedRatePercent()).isEqualByComparingTo("5");
        assertThat(at.appliedRatePercent()).isEqualByComparingTo("10");
        assertThat(above.appliedRatePercent()).isEqualByComparingTo("10");
        // Each amount is priced once, by exactly one band.
        assertThat(below.matchedTierTo()).isEqualByComparingTo("50000.00");
        assertThat(at.matchedTierFrom()).isEqualByComparingTo("50000.00");
    }

    @Test
    void zeroAndOpenEndedTopBandCoverTheWholeRange() {
        CommissionPlan plan = tieredPlan("0");
        List<CommissionTier> tiers = bands();

        assertThat(CommissionCalculator.quote(plan, money("0"), null, null, tiers)
                .commissionAmount()).isEqualByComparingTo("0.00");
        assertThat(CommissionCalculator.quote(plan, money("1000000000.00"), null, null, tiers)
                .commissionAmount()).isEqualByComparingTo("150000000.00");
    }

    @Test
    void minimumSalesThresholdIsInclusiveAndBasisAware() {
        CommissionPlan plan = tieredPlan("50000");
        List<CommissionTier> tiers = bands();

        // One rupee under the threshold: nothing owed, and the quote says why.
        CommissionQuote under = CommissionCalculator.quote(plan, money("49999.99"), null, null, tiers);
        assertThat(under.belowThreshold()).isTrue();
        assertThat(under.commissionAmount()).isEqualByComparingTo("0.00");
        assertThat(under.isOwed()).isFalse();

        // Exactly at the threshold is commissionable.
        CommissionQuote at = CommissionCalculator.quote(plan, money("50000"), null, null, tiers);
        assertThat(at.belowThreshold()).isFalse();
        assertThat(at.commissionAmount()).isEqualByComparingTo("5000.00");

        // The threshold is measured on the same basis the rate is applied to:
        // NET subtracts the discount, so 50000 gross - 5000 discount falls short.
        CommissionQuote discounted = CommissionCalculator.quote(plan, money("50000"), money("5000"), null, tiers);
        assertThat(discounted.basisAmount()).isEqualByComparingTo("45000.00");
        assertThat(discounted.belowThreshold()).isTrue();
    }

    @Test
    void percentPlanAppliesToNetNotGross() {
        CommissionPlan plan = new CommissionPlan();
        plan.setPlanKey("PERCENT_TEST");
        plan.setBasis(CommissionPlan.Basis.NET);
        plan.setMethod(CommissionPlan.Method.PERCENT);
        plan.setRatePercent(new BigDecimal("10.00"));

        // gross 20000 - 2000 discount + 1800 tax = 19800 -> 1980
        CommissionQuote net = CommissionCalculator.quote(plan, money("20000"), money("2000"), money("1800"), List.of());
        assertThat(net.basisAmount()).isEqualByComparingTo("19800.00");
        assertThat(net.commissionAmount()).isEqualByComparingTo("1980.00");
        assertThat(net.matchedTierFrom()).isNull();
    }

    @Test
    void grossBasisIgnoresDiscount() {
        CommissionPlan plan = new CommissionPlan();
        plan.setPlanKey("GROSS_TEST");
        plan.setBasis(CommissionPlan.Basis.GROSS);
        plan.setMethod(CommissionPlan.Method.PERCENT);
        plan.setRatePercent(new BigDecimal("10.00"));

        CommissionQuote gross = CommissionCalculator.quote(plan, money("20000"), money("5000"), money("1800"), List.of());
        assertThat(gross.basisAmount()).isEqualByComparingTo("20000.00");
        assertThat(gross.commissionAmount()).isEqualByComparingTo("2000.00");
    }

    @Test
    void fixedPlanPaysTheSameAmountWhateverTheBookingSize() {
        CommissionPlan plan = new CommissionPlan();
        plan.setPlanKey("FIXED_TEST");
        plan.setBasis(CommissionPlan.Basis.NET);
        plan.setMethod(CommissionPlan.Method.FIXED);
        plan.setFixedAmount(new BigDecimal("2500.00"));

        CommissionQuote small = CommissionCalculator.quote(plan, money("1000"), null, null, List.of());
        CommissionQuote large = CommissionCalculator.quote(plan, money("900000"), null, null, List.of());

        assertThat(small.commissionAmount()).isEqualByComparingTo("2500.00");
        assertThat(large.commissionAmount()).isEqualByComparingTo("2500.00");
        // A fixed plan has no percentage to report; the caller records 0 rather
        // than inventing one.
        assertThat(small.appliedRatePercent()).isNull();
    }

    @Test
    void commissionRoundsHalfUpAtScaleTwo() {
        CommissionPlan plan = new CommissionPlan();
        plan.setPlanKey("ROUND_TEST");
        plan.setBasis(CommissionPlan.Basis.NET);
        plan.setMethod(CommissionPlan.Method.PERCENT);
        plan.setRatePercent(new BigDecimal("12.50"));

        // 3333.33 * 12.5% = 416.666... -> 416.67 (half-up at the third decimal)
        assertThat(CommissionCalculator.quote(plan, money("3333.33"), null, null, List.of())
                .commissionAmount()).isEqualByComparingTo("416.67");
        // 0.05 * 12.5% = 0.00625 -> 0.01
        assertThat(CommissionCalculator.quote(plan, money("0.05"), null, null, List.of())
                .commissionAmount()).isEqualByComparingTo("0.01");
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}

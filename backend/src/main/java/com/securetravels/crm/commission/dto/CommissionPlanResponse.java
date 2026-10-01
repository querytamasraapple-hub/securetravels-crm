package com.securetravels.crm.commission.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A commission plan as returned by the API, including its tier bands. */
public record CommissionPlanResponse(
        UUID id,
        String key,
        String label,
        CommissionPlanBasis basis,
        CommissionPlanMethod method,
        BigDecimal ratePercent,
        BigDecimal fixedAmount,
        BigDecimal minSalesThreshold,
        boolean active,
        List<Tier> tiers,
        String notes
) {
    /** One band of a TIERED plan. {@code toAmount} is null for the top band. */
    public record Tier(
            UUID id,
            BigDecimal fromAmount,
            BigDecimal toAmount,
            BigDecimal ratePercent
    ) {
    }
}

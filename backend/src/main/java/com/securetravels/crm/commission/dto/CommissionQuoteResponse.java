package com.securetravels.crm.commission.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The computed answer to a quote request. When {@code planKey} is
 * {@code DEFAULT_FLAT_RATE} the account has no plan assigned and the flat
 * configured rate applies instead — the Module 1 behaviour.
 */
public record CommissionQuoteResponse(
        UUID accountId,
        UUID planId,
        String planKey,
        CommissionPlanBasis basis,
        CommissionPlanMethod method,
        BigDecimal basisAmount,
        BigDecimal commissionAmount,
        BigDecimal appliedRatePercent,
        BigDecimal matchedTierFrom,
        BigDecimal matchedTierTo,
        boolean belowThreshold,
        BigDecimal minSalesThreshold
) {
}

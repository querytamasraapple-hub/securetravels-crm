package com.securetravels.crm.commission.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One TIERED band: {@code ratePercent} applies to a commission basis in
 * {@code [fromAmount, toAmount)}. {@code toAmount} is null for the open-ended
 * top band; every other band must also close where the next one opens, which
 * {@code CommissionPlanService} validates so no booking value can fall
 * between bands and be paid at no rate.
 */
public record CommissionTierRequest(
        @NotNull(message = "fromAmount is required")
        @DecimalMin(value = "0.0", message = "fromAmount must not be negative")
        BigDecimal fromAmount,

        @DecimalMin(value = "0.0", message = "toAmount must not be negative")
        BigDecimal toAmount,

        @NotNull(message = "ratePercent is required")
        @DecimalMin(value = "0.0", message = "ratePercent must be between 0 and 100")
        @DecimalMax(value = "100.0", message = "ratePercent must be between 0 and 100")
        BigDecimal ratePercent
) {
}

package com.securetravels.crm.commission.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Commission plan update. Every field is optional, but {@code method} and
 * {@code basis} are effectively settled once a payable references the plan —
 * changing the method of a plan that has already been paid would reinterpret
 * historical amounts, so the service refuses that while the plan is assigned
 * or in use.
 */
public record CommissionPlanUpdateRequest(
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "key must be uppercase snake_case, starting with a letter")
        @Size(max = 40, message = "key too long")
        String key,

        @Size(max = 120, message = "label too long")
        String label,

        CommissionPlanBasis basis,

        CommissionPlanMethod method,

        @DecimalMin(value = "0.0", message = "ratePercent must be between 0 and 100")
        @DecimalMax(value = "100.0", message = "ratePercent must be between 0 and 100")
        BigDecimal ratePercent,

        @DecimalMin(value = "0.0", message = "fixedAmount must not be negative")
        BigDecimal fixedAmount,

        @DecimalMin(value = "0.0", message = "minSalesThreshold must not be negative")
        BigDecimal minSalesThreshold,

        @Size(max = 20, message = "at most 20 tiers per plan")
        @Valid
        java.util.List<@NotNull CommissionTierRequest> tiers,

        Boolean active,

        @Size(max = 2000, message = "notes too long")
        String notes
) {
}

package com.securetravels.crm.commission.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Commission plan creation. {@code planKey} becomes the immutable identity
 * once created. The method determines which inputs are required and the
 * service rejects combinations the DB would refuse (e.g. PERCENT with a fixed
 * amount), so the caller gets a 400 with a readable message instead of a
 * constraint violation.
 */
public record CommissionPlanCreateRequest(
        @NotBlank(message = "key is required")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "key must be uppercase snake_case, starting with a letter")
        @Size(max = 40, message = "key too long")
        String key,

        @NotBlank(message = "label is required")
        @Size(max = 120, message = "label too long")
        String label,

        @NotNull(message = "basis is required")
        CommissionPlanBasis basis,

        @NotNull(message = "method is required")
        CommissionPlanMethod method,

        @DecimalMin(value = "0.0", message = "ratePercent must be between 0 and 100")
        @DecimalMax(value = "100.0", message = "ratePercent must be between 0 and 100")
        BigDecimal ratePercent,

        @DecimalMin(value = "0.0", message = "fixedAmount must not be negative")
        BigDecimal fixedAmount,

        @DecimalMin(value = "0.0", message = "minSalesThreshold must not be negative")
        BigDecimal minSalesThreshold,

        /** Required and only meaningful for {@code TIERED} plans. */
        @Valid
        @Size(max = 20, message = "at most 20 tiers per plan")
        List<CommissionTierRequest> tiers,

        @Size(max = 2000, message = "notes too long")
        String notes
) {
}

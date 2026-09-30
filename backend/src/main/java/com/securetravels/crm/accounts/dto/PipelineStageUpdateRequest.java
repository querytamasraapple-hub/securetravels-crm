package com.securetravels.crm.accounts.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Partial update of a pipeline stage. {@code key} is immutable after create;
 * every other field is optional here. {@code active=false} deactivates a
 * stage (the service refuses to deactivate the last active one).
 */
public record PipelineStageUpdateRequest(
        @Size(max = 80, message = "label too long")
        String label,

        @Min(value = 0, message = "sortOrder must not be negative")
        @Max(value = 1000, message = "sortOrder too large")
        Integer sortOrder,

        @DecimalMin(value = "0.0", message = "probabilityWeight must be between 0 and 100")
        @DecimalMax(value = "100.0", message = "probabilityWeight must be between 0 and 100")
        BigDecimal probabilityWeight,

        @Size(max = 2000, message = "entryCondition too long")
        String entryCondition,

        Boolean active
) {
}
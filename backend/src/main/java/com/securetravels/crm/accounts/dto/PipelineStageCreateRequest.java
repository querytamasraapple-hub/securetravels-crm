package com.securetravels.crm.accounts.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Pipeline stage creation. {@code stageKey} becomes the immutable identity of
 * the stage once created (the Module 3 opportunity/forecast engine references
 * stages by it). Weights are 0.0–100.0 percent.
 */
public record PipelineStageCreateRequest(
        @NotBlank(message = "key is required")
        @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "key must be uppercase snake_case, starting with a letter")
        @Size(max = 40, message = "key too long")
        String key,

        @NotBlank(message = "label is required")
        @Size(max = 80, message = "label too long")
        String label,

        @NotNull(message = "sortOrder is required")
        @Min(value = 0, message = "sortOrder must not be negative")
        int sortOrder,

        @NotNull(message = "probabilityWeight is required")
        @DecimalMin(value = "0.0", message = "probabilityWeight must be between 0 and 100")
        @DecimalMax(value = "100.0", message = "probabilityWeight must be between 0 and 100")
        BigDecimal probabilityWeight,

        @Size(max = 2000, message = "entryCondition too long")
        String entryCondition,

        Boolean active
) {
}
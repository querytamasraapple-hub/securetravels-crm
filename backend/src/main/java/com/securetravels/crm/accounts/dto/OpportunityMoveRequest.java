package com.securetravels.crm.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Stage movement on an OPEN opportunity. The target is addressed by its
 * immutable {@code stageKey}; any active stage is eligible (a deal may drop
 * back a stage — forecast always weights with the live stage probability).
 */
public record OpportunityMoveRequest(
        @NotBlank(message = "stageKey is required")
        @Size(max = 40, message = "stageKey too long")
        String stageKey
) {
}
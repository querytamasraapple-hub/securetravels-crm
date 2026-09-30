package com.securetravels.crm.accounts.dto;

import com.securetravels.crm.accounts.Opportunity.Status;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Terminal transition of an OPEN opportunity: {@code WON} or {@code LOST}.
 * Irreversible — a closed opportunity is never reopened.
 */
public record OpportunityCloseRequest(
        @NotNull(message = "outcome is required")
        Status outcome,

        @Size(max = 2000, message = "note too long")
        String note
) {
}
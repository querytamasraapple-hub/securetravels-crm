package com.securetravels.crm.accounts.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Opportunity creation. An opportunity is always anchored to a lead (one per
 * lead — the DB UNIQUE is the dedup guard). {@code stageKey} is optional and
 * defaults to the first active pipeline stage; account, owner and value
 * defaults come from the lead.
 */
public record OpportunityCreateRequest(
        @NotNull(message = "leadId is required")
        UUID leadId,

        @Size(max = 40, message = "stageKey too long")
        String stageKey,

        @NotNull(message = "expectedValue is required")
        @DecimalMin(value = "0.0", message = "expectedValue must not be negative")
        BigDecimal expectedValue,

        @NotNull(message = "expectedDate is required")
        LocalDate expectedDate
) {
}
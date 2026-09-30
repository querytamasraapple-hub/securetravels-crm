package com.securetravels.crm.accounts.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only opportunity view with the live stage projection (key, label,
 * probability) resolved at read time from the configured pipeline.
 */
public record OpportunityResponse(
        UUID id,
        UUID leadId,
        UUID accountId,
        String stageKey,
        String stageLabel,
        BigDecimal probabilityWeight,
        UUID ownerId,
        BigDecimal expectedValue,
        LocalDate expectedDate,
        String status,
        Instant stageMovedAt,
        Instant closedAt,
        String closingNote,
        UUID closedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
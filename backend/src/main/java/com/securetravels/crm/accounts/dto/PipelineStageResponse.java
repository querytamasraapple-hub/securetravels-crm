package com.securetravels.crm.accounts.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only pipeline stage view. The list endpoint returns these ordered by
 * {@code sortOrder}.
 */
public record PipelineStageResponse(
        UUID id,
        String key,
        String label,
        int sortOrder,
        BigDecimal probabilityWeight,
        String entryCondition,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
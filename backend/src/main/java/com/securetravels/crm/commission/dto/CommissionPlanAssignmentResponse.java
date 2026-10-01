package com.securetravels.crm.commission.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * An account's active plan assignment. {@code planKey} is denormalised into
 * the response so a client does not need a second call to render the
 * account's current terms.
 */
public record CommissionPlanAssignmentResponse(
        UUID id,
        UUID accountId,
        UUID planId,
        String planKey,
        Instant assignedAt,
        UUID assignedBy
) {
}

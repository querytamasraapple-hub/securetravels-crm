package com.securetravels.crm.commission.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Assign a commission plan to an account. */
public record CommissionPlanAssignRequest(
        @NotNull(message = "planId is required")
        UUID planId
) {
}

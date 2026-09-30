package com.securetravels.crm.accounts.dto;

import jakarta.validation.constraints.Size;

public record PayableSettleRequest(
        @Size(max = 120, message = "paidRef too long")
        String paidRef
) {
}
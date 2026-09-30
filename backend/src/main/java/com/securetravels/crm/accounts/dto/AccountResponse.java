package com.securetravels.crm.accounts.dto;

import com.securetravels.crm.accounts.Account.AccountType;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        AccountType accountType,
        String name,
        String gstin,
        String billingName,
        String city,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
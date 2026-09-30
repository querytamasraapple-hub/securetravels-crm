package com.securetravels.crm.accounts.dto;

import com.securetravels.crm.accounts.Account.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Account field update (PATCH semantics: only provided fields change).
 * {@code gstin} and {@code activeBackoffice} style booleans implemented as
 * nullable boxed values so "not provided" is distinguishable.
 */
public record AccountUpdateRequest(
        AccountType accountType,

        @Size(max = 200, message = "name too long")
        String name,

        @Size(max = 15, message = "gstin too long")
        String gstin,

        @Size(max = 200, message = "billingName too long")
        String billingName,

        @Size(max = 1000, message = "billingAddress too long")
        String billingAddress,

        @Size(max = 80, message = "city too long")
        String city,

        @Size(max = 200, message = "primaryContactName too long")
        String primaryContactName,

        @Email(message = "primaryContactEmail must be valid")
        @Size(max = 255)
        String primaryContactEmail,

        @Size(max = 30, message = "primaryContactPhone too long")
        String primaryContactPhone,

        @Size(max = 40, message = "bankAccountRef too long")
        String bankAccountRef,

        @Min(value = 0, message = "creditTermsDays must not be negative")
        Integer creditTermsDays,

        @Size(max = 2000, message = "notes too long")
        String notes,

        Boolean active
) {
}
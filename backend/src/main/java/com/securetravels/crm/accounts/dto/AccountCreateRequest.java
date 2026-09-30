package com.securetravels.crm.accounts.dto;

import com.securetravels.crm.accounts.Account.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Account creation. {@code gstin} is optional but, when supplied, must be a
 * structurally valid Indian GSTIN — the service enforces the checksum.
 */
public record AccountCreateRequest(
        @NotNull(message = "accountType is required")
        AccountType accountType,

        @NotBlank(message = "name is required")
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
        String notes
) {
}
package com.securetravels.crm.commission.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * A "what would this pay?" question about an account's current terms. Answers
 * are computed with exactly the code that runs at booking confirmation, so a
 * manager can check a partner's rate without staging a booking.
 */
public record CommissionQuoteRequest(
        @NotNull(message = "grossAmount is required")
        @DecimalMin(value = "0.0", message = "grossAmount must not be negative")
        BigDecimal grossAmount,

        @DecimalMin(value = "0.0", message = "discountAmount must not be negative")
        BigDecimal discountAmount,

        @DecimalMin(value = "0.0", message = "taxAmount must not be negative")
        BigDecimal taxAmount
) {
}

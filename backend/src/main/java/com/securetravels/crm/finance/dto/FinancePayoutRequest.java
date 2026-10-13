package com.securetravels.crm.finance.dto;

import com.securetravels.crm.finance.FinanceVendorPayout;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FinancePayoutRequest(
        @NotBlank @Size(max = 160) String vendorName,
        @NotNull FinanceVendorPayout.Category category,
        UUID tripId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate payoutDate,
        LocalDate dueDate,
        FinanceVendorPayout.PaymentMethod paymentMethod,
        FinanceVendorPayout.Status status,
        @Size(max = 80) String referenceNo,
        @Size(max = 2000) String notes
) {}

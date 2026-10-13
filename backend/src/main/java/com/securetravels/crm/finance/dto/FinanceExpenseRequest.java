package com.securetravels.crm.finance.dto;

import com.securetravels.crm.finance.FinanceExpense;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FinanceExpenseRequest(
        @NotNull FinanceExpense.Category category,
        @NotBlank @Size(max = 200) String title,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate expenseDate,
        UUID tripId,
        @Size(max = 160) String vendorName,
        FinanceExpense.PaymentMethod paymentMethod,
        FinanceExpense.Status status,
        @Size(max = 2000) String notes
) {}

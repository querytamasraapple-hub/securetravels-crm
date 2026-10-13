package com.securetravels.crm.finance.dto;

import com.securetravels.crm.finance.FinanceExpense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FinanceExpenseResponse(
        UUID id,
        FinanceExpense.Category category,
        String title,
        BigDecimal amount,
        LocalDate expenseDate,
        UUID tripId,
        String tripName,
        String vendorName,
        FinanceExpense.PaymentMethod paymentMethod,
        FinanceExpense.Status status,
        String notes,
        UUID recordedBy,
        Instant createdAt
) {
    public static FinanceExpenseResponse from(FinanceExpense e, String tripName) {
        return new FinanceExpenseResponse(
                e.getId(), e.getCategory(), e.getTitle(), e.getAmount(), e.getExpenseDate(),
                e.getTripId(), tripName, e.getVendorName(), e.getPaymentMethod(), e.getStatus(),
                e.getNotes(), e.getRecordedBy(), e.getCreatedAt());
    }
}

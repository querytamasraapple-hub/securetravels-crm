package com.securetravels.crm.finance.dto;

import com.securetravels.crm.finance.FinanceVendorPayout;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FinancePayoutResponse(
        UUID id,
        String vendorName,
        FinanceVendorPayout.Category category,
        UUID tripId,
        String tripName,
        BigDecimal amount,
        LocalDate payoutDate,
        LocalDate dueDate,
        FinanceVendorPayout.PaymentMethod paymentMethod,
        FinanceVendorPayout.Status status,
        String referenceNo,
        String notes,
        Instant createdAt
) {
    public static FinancePayoutResponse from(FinanceVendorPayout p, String tripName) {
        return new FinancePayoutResponse(
                p.getId(), p.getVendorName(), p.getCategory(), p.getTripId(), tripName,
                p.getAmount(), p.getPayoutDate(), p.getDueDate(), p.getPaymentMethod(),
                p.getStatus(), p.getReferenceNo(), p.getNotes(), p.getCreatedAt());
    }
}

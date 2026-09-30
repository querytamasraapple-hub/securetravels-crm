package com.securetravels.crm.accounts.dto;

import com.securetravels.crm.accounts.Account.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Account 360 — the audit-of-record view of one account: master data,
 * lifetime aggregates, recent bookings and its commission payables.
 */
public record AccountDetailResponse(
        UUID id,
        AccountType accountType,
        String name,
        String gstin,
        String billingName,
        String billingAddress,
        String city,
        String primaryContactName,
        String primaryContactEmail,
        String primaryContactPhone,
        String bankAccountRef,
        Integer creditTermsDays,
        String notes,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Stats stats,
        List<BookingSummary> recentBookings,
        List<PayableSummary> payables
) {
    public record Stats(
            long leads,
            long bookings,
            long confirmedBookings,
            BigDecimal grossRevenue,
            BigDecimal openCommission,
            BigDecimal paidCommission
    ) {
    }

    public record BookingSummary(
            UUID bookingId,
            String bookingRef,
            String status,
            LocalDate travelDate,
            BigDecimal netAmount
    ) {
    }

    public record PayableSummary(
            UUID payableId,
            UUID bookingId,
            String bookingRef,
            String status,
            BigDecimal commissionAmount,
            Instant payableAt
    ) {
    }
}
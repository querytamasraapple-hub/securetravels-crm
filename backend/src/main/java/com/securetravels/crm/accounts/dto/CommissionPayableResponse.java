package com.securetravels.crm.accounts.dto;

import com.securetravels.crm.accounts.AccountCommissionPayable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A partner-commission payable. {@code planId} is the Module 4 plan that
 * produced the amount, or null for a payable written under the flat default
 * rate.
 */
public record CommissionPayableResponse(
        UUID id,
        UUID bookingId,
        UUID accountId,
        UUID planId,
        Instant payableAt,
        AccountCommissionPayable.Basis basis,
        BigDecimal ratePercent,
        BigDecimal grossAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal netAmount,
        BigDecimal commissionAmount,
        AccountCommissionPayable.Status status,
        Instant paidAt,
        String paidRef,
        String notes
) {
}
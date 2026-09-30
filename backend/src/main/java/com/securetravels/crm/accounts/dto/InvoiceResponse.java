package com.securetravels.crm.accounts.dto;

import com.securetravels.crm.accounts.AccountInvoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceRef,
        UUID bookingId,
        UUID accountId,
        AccountInvoice.BillingEntity billingEntity,
        String billingName,
        String billingAddress,
        String billingGstin,
        BigDecimal grossAmount,
        BigDecimal discountAmount,
        BigDecimal taxAmount,
        BigDecimal netAmount,
        AccountInvoice.Status status,
        Instant issuedAt,
        String notes
) {
}
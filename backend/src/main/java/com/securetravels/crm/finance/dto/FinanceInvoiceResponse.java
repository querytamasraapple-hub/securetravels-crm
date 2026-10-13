package com.securetravels.crm.finance.dto;

import com.securetravels.crm.finance.FinanceInvoice;
import com.securetravels.crm.finance.FinanceInvoiceLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FinanceInvoiceResponse(
        UUID id,
        String invoiceNo,
        UUID bookingId,
        String customerName,
        String customerEmail,
        String customerWhatsapp,
        String customerPhone,
        String customerGstin,
        String customerAddress,
        String customerState,
        String customerStateCode,
        String placeOfSupply,
        String placeOfSupplyCode,
        String sellerLegalName,
        String sellerTradeName,
        String sellerGstin,
        String sellerAddress,
        String sellerState,
        String sellerStateCode,
        String sellerPhone,
        String sellerEmail,
        String sellerBankName,
        String sellerBankAccount,
        String sellerBankIfsc,
        LocalDate issueDate,
        LocalDate dueDate,
        FinanceInvoice.InvoiceType invoiceType,
        FinanceInvoice.SupplyType supplyType,
        boolean reverseCharge,
        String sacCode,
        BigDecimal subtotal,
        BigDecimal cgstRate,
        BigDecimal sgstRate,
        BigDecimal igstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String amountInWords,
        String currency,
        FinanceInvoice.Status status,
        String notes,
        String declaration,
        FinanceInvoice.WhatsappDeliveryStatus whatsappDeliveryStatus,
        Instant whatsappQueuedAt,
        Instant whatsappSentAt,
        String whatsappLastError,
        List<Line> lines,
        Instant createdAt
) {
    public record Line(
            UUID id,
            int lineNo,
            String description,
            String hsnSac,
            BigDecimal quantity,
            String unit,
            BigDecimal rate,
            BigDecimal taxableAmount,
            BigDecimal gstRate
    ) {}

    public static FinanceInvoiceResponse from(FinanceInvoice i) {
        return from(i, i.getStatus());
    }

    public static FinanceInvoiceResponse from(FinanceInvoice i, FinanceInvoice.Status displayStatus) {
        List<Line> lines = i.getLines().stream()
                .map(FinanceInvoiceResponse::toLine)
                .toList();
        return new FinanceInvoiceResponse(
                i.getId(), i.getInvoiceNo(), i.getBookingId(),
                i.getCustomerName(), i.getCustomerEmail(), i.getCustomerWhatsapp(), i.getCustomerPhone(),
                i.getCustomerGstin(), i.getCustomerAddress(), i.getCustomerState(), i.getCustomerStateCode(),
                i.getPlaceOfSupply(), i.getPlaceOfSupplyCode(),
                i.getSellerLegalName(), i.getSellerTradeName(), i.getSellerGstin(), i.getSellerAddress(),
                i.getSellerState(), i.getSellerStateCode(), i.getSellerPhone(), i.getSellerEmail(),
                i.getSellerBankName(), i.getSellerBankAccount(), i.getSellerBankIfsc(),
                i.getIssueDate(), i.getDueDate(), i.getInvoiceType(), i.getSupplyType(), i.isReverseCharge(),
                i.getSacCode(), i.getSubtotal(),
                i.getCgstRate(), i.getSgstRate(), i.getIgstRate(),
                i.getCgstAmount(), i.getSgstAmount(), i.getIgstAmount(),
                i.getTaxAmount(), i.getTotalAmount(), i.getAmountInWords(), i.getCurrency(),
                displayStatus, i.getNotes(), i.getDeclaration(),
                i.getWhatsappDeliveryStatus(), i.getWhatsappQueuedAt(), i.getWhatsappSentAt(),
                i.getWhatsappLastError(), lines, i.getCreatedAt());
    }

    private static Line toLine(FinanceInvoiceLine l) {
        return new Line(l.getId(), l.getLineNo(), l.getDescription(), l.getHsnSac(), l.getQuantity(),
                l.getUnit(), l.getRate(), l.getTaxableAmount(), l.getGstRate());
    }
}

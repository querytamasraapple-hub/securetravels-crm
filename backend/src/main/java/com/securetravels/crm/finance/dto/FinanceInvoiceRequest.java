package com.securetravels.crm.finance.dto;

import com.securetravels.crm.finance.FinanceInvoice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FinanceInvoiceRequest(
        UUID bookingId,
        @NotBlank @Size(max = 160) String customerName,
        @Size(max = 255) String customerEmail,
        @NotBlank @Size(max = 20) String customerWhatsapp,
        @Size(max = 20) String customerPhone,
        @Size(max = 20) String customerGstin,
        @Size(max = 1000) String customerAddress,
        @Size(max = 80) String customerState,
        @Size(max = 2) String customerStateCode,
        @Size(max = 80) String placeOfSupply,
        @Size(max = 2) String placeOfSupplyCode,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate dueDate,
        FinanceInvoice.InvoiceType invoiceType,
        FinanceInvoice.SupplyType supplyType,
        Boolean reverseCharge,
        @Size(max = 12) String sacCode,
        FinanceInvoice.Status status,
        @Size(max = 2000) String notes,
        @Size(max = 2000) String declaration,
        Boolean queueWhatsapp,
        @Valid @NotEmpty List<FinanceInvoiceLineRequest> lines
) {}

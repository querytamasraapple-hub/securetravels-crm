package com.securetravels.crm.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record FinanceInvoiceLineRequest(
        @NotBlank @Size(max = 400) String description,
        @Size(max = 12) String hsnSac,
        @NotNull @DecimalMin("0.01") BigDecimal quantity,
        @Size(max = 20) String unit,
        @NotNull @DecimalMin("0.01") BigDecimal rate,
        @DecimalMin("0") BigDecimal gstRate
) {}

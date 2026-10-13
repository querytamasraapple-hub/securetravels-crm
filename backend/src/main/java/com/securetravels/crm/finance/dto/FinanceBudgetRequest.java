package com.securetravels.crm.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record FinanceBudgetRequest(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String periodMonth,
        @NotBlank @Size(max = 40) String category,
        @NotNull @DecimalMin("0") BigDecimal budgetAmount,
        @Size(max = 2000) String notes
) {}

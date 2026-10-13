package com.securetravels.crm.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinanceBudgetResponse(
        String periodMonth,
        List<BudgetLine> lines,
        BigDecimal totalBudget,
        BigDecimal totalActual,
        BigDecimal variance
) {
    public record BudgetLine(
            String category,
            BigDecimal budgetAmount,
            BigDecimal actualAmount,
            BigDecimal variance,
            BigDecimal usedPct
    ) {}
}

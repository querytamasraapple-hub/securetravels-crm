package com.securetravels.crm.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinancePnLResponse(
        String from,
        String to,
        BigDecimal revenue,
        BigDecimal expenses,
        BigDecimal grossProfit,
        BigDecimal marginPct,
        List<FinanceOverviewResponse.NamedAmount> expenseLines,
        List<FinanceOverviewResponse.NamedAmount> revenueByTrip
) {}

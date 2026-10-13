package com.securetravels.crm.finance.dto;

import java.math.BigDecimal;
import java.util.List;

/** GST-oriented tax summary for Indian tour packages (default 5% on collected). */
public record FinanceTaxResponse(
        String from,
        String to,
        BigDecimal taxableRevenue,
        BigDecimal outputGst,
        BigDecimal estimatedInputCredit,
        BigDecimal netGstPayable,
        BigDecimal gstRatePct,
        List<FinanceOverviewResponse.NamedAmount> taxableByTrip
) {}

package com.securetravels.crm.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinanceCashflowResponse(
        String from,
        String to,
        BigDecimal totalInflow,
        BigDecimal totalOutflow,
        BigDecimal netCash,
        List<DayPoint> daily
) {
    public record DayPoint(String date, BigDecimal inflow, BigDecimal outflow, BigDecimal net) {}
}

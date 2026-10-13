package com.securetravels.crm.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinanceAgingResponse(
        BigDecimal current,
        BigDecimal days1to30,
        BigDecimal days31to60,
        BigDecimal days61to90,
        BigDecimal days90plus,
        BigDecimal totalOutstanding,
        List<AgingLine> lines
) {
    public record AgingLine(
            String bookingRef,
            String amountType,
            BigDecimal amount,
            String dueDate,
            int daysPastDue,
            String bucket,
            String status
    ) {}
}

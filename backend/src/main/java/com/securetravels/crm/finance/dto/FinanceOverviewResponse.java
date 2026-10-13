package com.securetravels.crm.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinanceOverviewResponse(
        BigDecimal collectedRevenue,
        BigDecimal outstandingReceivables,
        BigDecimal overdueAmount,
        BigDecimal refundedAmount,
        BigDecimal bookingPipeline,
        BigDecimal totalExpenses,
        BigDecimal netPosition,
        BigDecimal collectionRatePct,
        long completedPaymentCount,
        long overduePaymentCount,
        long confirmedBookingCount,
        List<MonthPoint> monthlySeries,
        List<NamedAmount> revenueByTrip,
        List<NamedAmount> expensesByCategory,
        List<StatusBucket> paymentsByStatus
) {
    public record MonthPoint(String month, BigDecimal collected, BigDecimal expenses, BigDecimal outstanding) {}
    public record NamedAmount(String name, BigDecimal amount, long count) {}
    public record StatusBucket(String status, BigDecimal amount, long count) {}
}

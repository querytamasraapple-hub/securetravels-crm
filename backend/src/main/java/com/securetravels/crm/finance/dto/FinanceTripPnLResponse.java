package com.securetravels.crm.finance.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FinanceTripPnLResponse(List<TripRow> trips) {
    public record TripRow(
            UUID tripId,
            String tripName,
            long bookingCount,
            BigDecimal revenue,
            BigDecimal expenses,
            BigDecimal payouts,
            BigDecimal profit,
            BigDecimal marginPct
    ) {}
}

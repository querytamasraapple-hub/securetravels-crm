package com.securetravels.crm.accounts.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Revenue forecast for the half-open window {@code [from, to)} on
 * {@code expected_date}:
 *
 * <ul>
 *   <li>{@code expected} — sum of {@code expected_value × stage probability}
 *       over OPEN opportunities (the weighted / expected-value view);</li>
 *   <li>{@code best} — full sum of OPEN {@code expected_value} (the pipeline
 *       at 100% / best-case view);</li>
 *   <li>{@code won} — sum of {@code expected_value} over WON opportunities.</li>
 * </ul>
 *
 * Stage buckets break the OPEN pipeline down by stage; month buckets break
 * expected / best / won by the expected-date month.
 */
public record ForecastResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal expected,
        BigDecimal best,
        BigDecimal won,
        long openCount,
        List<StageBucket> byStage,
        List<MonthBucket> byMonth
) {
    public record StageBucket(
            String stageKey,
            String stageLabel,
            BigDecimal probabilityWeight,
            long count,
            BigDecimal expected,
            BigDecimal best
    ) {
    }

    public record MonthBucket(
            String month,
            BigDecimal expected,
            BigDecimal best,
            BigDecimal won
    ) {
    }
}
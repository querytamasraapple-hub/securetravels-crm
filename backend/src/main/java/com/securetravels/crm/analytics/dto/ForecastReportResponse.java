package com.securetravels.crm.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Phase 7 Module 5 - forecast report: monthly buckets plus the two cuts a
 * manager actually asks for (by owner, by stage).
 *
 * <p>Module 3's {@code GET /api/forecast} returns stage and month buckets. This
 * adds the owner cut and the stage <em>mix</em> (share of expected value per
 * stage), and states explicitly what is left out, so the number can be trusted:
 *
 * <ul>
 *   <li>{@code openOutsideWindow} counts OPEN deals whose expected date falls
 *       outside {@code [from, to)}. They are excluded from the buckets, because
 *       putting a deal in a month it is not expected in would invent a commit.</li>
 *   <li>Weighted value uses the stage's current {@code probability_weight}, so
 *       re-weighting a stage moves this report immediately. No rollup table.</li>
 *   <li>{@code pipelineCoverage} is the sum of weights of stages holding open
 *       value; below 100% means part of the pipeline sits in stages that cannot
 *       be won.</li>
 * </ul>
 *
 * <p>Scoping is the caller's: a SALES user sees only their own pipeline in every
 * section, including the owner cut, which therefore returns a single row.
 */
public record ForecastReportResponse(
        String scope,
        UUID ownerFilter,
        LocalDate from,
        LocalDate to,
        ValueBasis valueBasis,
        List<Month> months,
        List<Owner> owners,
        List<StageMix> stageMix,
        Totals totals) {

    public record ValueBasis(
            String expected,
            String best,
            String won,
            boolean windowHalfOpen,
            boolean liveWeighted) {
    }

    public record Month(
            String month,
            long openCount,
            BigDecimal expected,
            BigDecimal best,
            long wonCount,
            BigDecimal won) {
    }

    public record Owner(
            UUID ownerId,
            String fullName,
            long openCount,
            BigDecimal openValue,
            BigDecimal expected,
            long wonCount,
            BigDecimal won,
            BigDecimal expectedSharePct) {
    }

    public record StageMix(
            String stageKey,
            String label,
            BigDecimal probabilityWeight,
            long openCount,
            BigDecimal openValue,
            BigDecimal expected,
            BigDecimal expectedSharePct) {
    }

    public record Totals(
            long openInWindow,
            long openOutsideWindow,
            BigDecimal openValue,
            BigDecimal expected,
            BigDecimal best,
            long wonCount,
            BigDecimal won,
            BigDecimal pipelineCoveragePct) {
    }
}
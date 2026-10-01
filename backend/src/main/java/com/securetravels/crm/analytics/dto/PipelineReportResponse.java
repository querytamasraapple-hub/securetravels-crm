package com.securetravels.crm.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Phase 7 Module 5 - pipeline health, per stage.
 *
 * <p>Answers "where is the pipeline stuck", which is a different question from
 * Module 3's {@code GET /api/forecast} ("what is it worth"). The forecast sums
 * value; this report counts deals and measures how long they have sat still.
 *
 * <p>Scoping: a SALES caller is pinned to their own opportunities regardless of
 * any parameter, exactly as {@code OpportunityService.list} does. OPS and
 * managers see the whole pipeline, matching {@code AnalyticsService.SEES_ALL}.
 */
public record PipelineReportResponse(
        String scope,
        UUID ownerFilter,
        LocalDate from,
        LocalDate to,
        Integer staleAfterDays,
        List<Stage> stages,
        Totals totals) {

    /**
     * @param avgDaysInStage  mean age of the open deals in this stage, or null
     *                        when the stage holds no open deal. Null rather than 0
     *                        so "nothing here" is not read as "moves instantly".
     * @param staleOpenDeals  open deals whose last stage move is at least
     *                        {@code staleAfterDays} old
     */
    public record Stage(
            UUID stageId,
            String stageKey,
            String label,
            int sortOrder,
            BigDecimal probabilityWeight,
            boolean active,
            long openCount,
            BigDecimal openValue,
            BigDecimal weightedValue,
            BigDecimal avgDaysInStage,
            long staleOpenDeals,
            long wonCount,
            BigDecimal wonValue,
            long lostCount,
            BigDecimal lostValue,
            BigDecimal winRatePct) {
    }

    /**
     * Won value only counts deals that closed inside the window; open value is
     * not window-limited by expected_date because an open deal's expected date is
     * a target, not an event.
     *
     * @param openOutsideWindow  open deals the window excluded, so the stage rows
     *                           are never mistaken for the whole pipeline. Always 0
     *                           when no window was supplied, since an unwindowed
     *                           report excludes nothing.
     */
    public record Totals(
            long openCount,
            BigDecimal openValue,
            BigDecimal weightedValue,
            long openOutsideWindow,
            long wonCount,
            BigDecimal wonValue,
            long lostCount,
            BigDecimal lostValue,
            long staleOpenDeals,
            BigDecimal winRatePct) {
    }
}
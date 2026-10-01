package com.securetravels.crm.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Phase 7 Module 5 - partner commission report (per account and per plan).
 *
 * <p>Reads {@code account_commission_payables}, the accrual ledger from
 * Modules 1 and 4. Money owed to an agent is reported by status rather than net:
 * accrued, paid, still open, voided. A netted figure would quietly absorb a
 * cancelled booking, and the whole point of VOID-not-delete is that a reversal
 * stays visible.
 *
 * <p>{@code planId} is nullable on a payable: null means the Module 1 flat
 * fallback paid it, and those rows are grouped under {@code DEFAULT_FLAT_RATE}
 * rather than dropped, so switching an account onto a plan cannot make earlier
 * commission disappear from a per-plan rollup.
 *
 * <p>Manager-and-up only, enforced in {@code SalesReportingService}, not by the
 * controller annotation alone.
 */
public record PartnerCommissionReportResponse(
        LocalDate from,
        LocalDate to,
        String windowBasis,
        List<Account> accounts,
        List<Plan> plans,
        Totals totals) {

    /**
     * @param openLiability   what we still owe on this account right now
     * @param effectiveRate   the rate the most recent payable was accrued at, and
     *                        {@code null} when the account has no accrual yet
     */
    public record Account(
            UUID accountId,
            String accountName,
            UUID planId,
            String planKey,
            String planBasis,
            long payables,
            BigDecimal accrued,
            BigDecimal paid,
            BigDecimal openLiability,
            long voided,
            BigDecimal voidedAmount,
            String effectiveRate,
            BigDecimal effectiveRatePercent) {
    }

    public record Plan(
            UUID planId,
            String planKey,
            String label,
            String method,
            String basis,
            long activeAccounts,
            long payables,
            BigDecimal accrued,
            BigDecimal paid,
            BigDecimal openLiability) {
    }

    public record Totals(
            long accounts,
            long payables,
            BigDecimal accrued,
            BigDecimal paid,
            BigDecimal openLiability,
            BigDecimal voided,
            BigDecimal paidSharePct,
            BigDecimal avgCommissionPerPayable) {
    }
}
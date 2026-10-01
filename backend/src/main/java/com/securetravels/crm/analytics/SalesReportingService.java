package com.securetravels.crm.analytics;

import com.securetravels.crm.analytics.dto.ForecastReportResponse;
import com.securetravels.crm.analytics.dto.PartnerCommissionReportResponse;
import com.securetravels.crm.analytics.dto.PipelineReportResponse;
import com.securetravels.crm.common.exception.BadRequestException;
import com.securetravels.crm.common.exception.ForbiddenException;
import com.securetravels.crm.user.Role;
import com.securetravels.crm.user.UserPrincipal;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Phase 7 Module 5 - sales reporting: pipeline health, forecast, and partner
 * commission.
 *
 * <p>Separate from {@link AnalyticsService} on purpose. The Phase 3 Module 2
 * reports are lead/trip/team shaped and share one filter object; these three read
 * the Phase 7 tables ({@code opportunities}, {@code account_commission_payables})
 * and each has its own visibility rule, so folding them into the existing service
 * would mean one 1400-line class with two different scoping policies in it.
 *
 * <p>Everything is computed live from the source tables. There is no rollup or
 * aggregate table, for the reason Module 3 gave and V23 repeats: re-weighting a
 * stage or changing a commission plan must move the report immediately, and a
 * stored total is a second source of truth that can only drift from the rows it
 * summarises. Indexes in V23 keep the scans cheap; correctness does not depend on
 * them.
 *
 * <p>Visibility, and why it differs per report:
 *
 * <ul>
 *   <li><strong>Pipeline and forecast</strong> follow {@code AnalyticsService}:
 *       SALES is pinned to their own rows, OPS and above see everything. A
 *       pipeline is not money, and an account manager who can see their own
 *       pipeline but not a colleague's is the correct behaviour.</li>
 *   <li><strong>Partner commission</strong> is manager-and-up. It is money owed
 *       to a third party, and Module 4 already settled that this data is not
 *       sales-visible; a report that aggregated it would reopen that hole in
 *       aggregate form.</li>
 * </ul>
 */
@Service
public class SalesReportingService {

    /** Roles that see every consultant's pipeline. Matches AnalyticsService. */
    private static final Set<Role> SEES_ALL = EnumSet.of(Role.OPS, Role.MANAGER, Role.ADMIN, Role.CEO);

    /** Money owed to a partner is manager-and-up only. */
    private static final Set<Role> MANAGER_AND_UP = EnumSet.of(Role.MANAGER, Role.ADMIN, Role.CEO);

    private static final ZoneId ZONE = ZoneId.systemDefault();

    /** A deal nobody has touched in this long is reported as stale. */
    private static final int DEFAULT_STALE_AFTER_DAYS = 30;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final NamedParameterJdbcTemplate jdbc;

    public SalesReportingService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ==================================================================
    // 1. Pipeline report
    // ==================================================================

    /**
     * Pipeline health per stage: how much value sits there, how long it has been
     * sitting, and how much of it actually converts.
     *
     * <p>The window filters opportunities by <em>expected date</em>, not creation
     * date: the question is "what is due to land in this period". Terminal deals
     * (WON/LOST) are counted by {@code closed_at} instead, because a deal that
     * closed in the window is what the window is about even if its expected date
     * was earlier. Open deals with no expected date in the window are excluded
     * from the stage rows entirely and reported as {@code openOutsideWindow} in
     * the totals, so nothing vanishes silently.
     *
     * <p>The window itself is optional: omit both bounds and this reports the
     * whole book, which is what a health check wants and what a client should not
     * have to compute a date range to get.
     */
    @Transactional(readOnly = true)
    public PipelineReportResponse pipelineReport(UUID ownerId,
                                                 LocalDate from,
                                                 LocalDate to,
                                                 Integer staleAfterDays,
                                                 UserPrincipal caller) {
        validateOptionalWindow(from, to);
        int staleDays = staleAfterDays == null ? DEFAULT_STALE_AFTER_DAYS : staleAfterDays;
        if (staleDays < 1 || staleDays > 365) {
            throw new BadRequestException("staleAfterDays must be between 1 and 365");
        }
        UUID effectiveOwner = SEES_ALL.contains(caller.role()) ? ownerId : caller.id();

        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("staleCutoff", Timestamp.from(LocalDate.now(ZONE).minusDays(staleDays).atStartOfDay(ZONE).toInstant()));

        StringBuilder scope = new StringBuilder();
        if (effectiveOwner != null) {
            scope.append(" and o.owner_id = :ownerId");
            p.addValue("ownerId", effectiveOwner);
        }
        String window = from == null ? "" : """
                  and ((o.status = 'OPEN' and o.expected_date >= :from and o.expected_date < :to)
                       or (o.status <> 'OPEN' and o.closed_at >= :closedFrom and o.closed_at < :closedTo))
                """;
        if (from != null) {
            p.addValue("from", from);
            p.addValue("to", to);
            p.addValue("closedFrom", Timestamp.from(from.atStartOfDay(ZONE).toInstant()));
            p.addValue("closedTo", Timestamp.from(to.atStartOfDay(ZONE).toInstant()));
        }

        String sql = """
                select s.id                                   as stage_id,
                       s.stage_key                            as stage_key,
                       s.label                                as label,
                       s.sort_order                           as sort_order,
                       s.probability_weight                   as weight,
                       s.is_active                            as active,
                       count(o.id) filter (where o.status = 'OPEN')                    as open_count,
                       coalesce(sum(o.expected_value)
                           filter (where o.status = 'OPEN'), 0)                        as open_value,
                       coalesce(sum(o.expected_value * s.probability_weight / 100)
                           filter (where o.status = 'OPEN'), 0)                        as weighted_value,
                       avg(extract(epoch from (now() - o.stage_moved_at)) / 86400.0)
                           filter (where o.status = 'OPEN')                             as avg_days,
                       count(o.id) filter (where o.status = 'OPEN'
                                            and o.stage_moved_at < :staleCutoff)        as stale_open,
                       count(o.id) filter (where o.status = 'WON')                     as won_count,
                       coalesce(sum(o.expected_value)
                           filter (where o.status = 'WON'), 0)                         as won_value,
                       count(o.id) filter (where o.status = 'LOST')                    as lost_count,
                       coalesce(sum(o.expected_value)
                           filter (where o.status = 'LOST'), 0)                        as lost_value
                  from pipeline_stages s
                  -- Driven from the stage list, left joined, so an empty stage still
                  -- gets a row: a health board that hides the stage nobody is
                  -- quoting in is hiding the problem. The scope and window live in
                  -- the ON clause rather than WHERE for the same reason - in WHERE
                  -- they would silently turn this back into an inner join for a
                  -- scoped caller.
             left join opportunities o
                    on o.stage_id = s.id %s %s
                 group by s.id, s.stage_key, s.label, s.sort_order, s.probability_weight, s.is_active
                 order by s.sort_order, s.stage_key
                """.formatted(window, scope);

        List<Map<String, Object>> rows = jdbc.queryForList(sql, p);

        List<PipelineReportResponse.Stage> stages = new ArrayList<>();
        long totalOpen = 0;
        long totalStale = 0;
        long totalWon = 0;
        long totalLost = 0;
        BigDecimal openValue = money(0);
        BigDecimal weighted = money(0);
        BigDecimal wonValue = money(0);
        BigDecimal lostValue = money(0);

        for (Map<String, Object> r : rows) {
            long open = asLong(r.get("open_count"));
            long won = asLong(r.get("won_count"));
            long lost = asLong(r.get("lost_count"));
            long stale = asLong(r.get("stale_open"));
            BigDecimal stageOpenValue = money(r.get("open_value"));
            BigDecimal stageWeighted = money(r.get("weighted_value"));
            BigDecimal stageWon = money(r.get("won_value"));
            BigDecimal stageLost = money(r.get("lost_value"));
            Object avgDays = r.get("avg_days");

            stages.add(new PipelineReportResponse.Stage(
                    (UUID) r.get("stage_id"),
                    (String) r.get("stage_key"),
                    (String) r.get("label"),
                    (int) asLong(r.get("sort_order")),
                    money(r.get("weight")),
                    Boolean.TRUE.equals(r.get("active")),
                    open, stageOpenValue, stageWeighted,
                    avgDays == null ? null : BigDecimal.valueOf(asDouble(avgDays)).setScale(1, RoundingMode.HALF_UP),
                    stale, won, stageWon, lost, stageLost,
                    pct(won, won + lost)));

            totalOpen += open;
            totalStale += stale;
            totalWon += won;
            totalLost += lost;
            openValue = openValue.add(stageOpenValue);
            weighted = weighted.add(stageWeighted);
            wonValue = wonValue.add(stageWon);
            lostValue = lostValue.add(stageLost);
        }

        // Open deals whose expected date is outside the window are counted in the
        // totals so the reader can see the stage rows are not the whole pipeline.
        // With no window there is nothing to be outside of, so the count is zero by
        // definition -- running the query without the exclusion clause instead
        // would report every open deal as "excluded", which is the one number here
        // that must never be a lie.
        long openOutsideWindow = 0;
        if (from != null) {
            StringBuilder outsideScope = new StringBuilder();
            if (effectiveOwner != null) {
                outsideScope.append(" and o.owner_id = :ownerId");
            }
            String outsideSql = """
                    select count(*) as n
                      from opportunities o
                     where o.status = 'OPEN'
                       and not (o.expected_date >= :from and o.expected_date < :to) %s
                    """.formatted(outsideScope);
            openOutsideWindow = asLong(jdbc.queryForMap(outsideSql, p).get("n"));
        }

        PipelineReportResponse.Totals totals = new PipelineReportResponse.Totals(
                totalOpen, openValue, weighted, openOutsideWindow, totalWon, wonValue,
                totalLost, lostValue, totalStale, pct(totalWon, totalWon + totalLost));

        return new PipelineReportResponse(scopeLabel(caller), effectiveOwner, from, to, staleDays, stages, totals);
    }

    // ==================================================================
    // 2. Forecast report
    // ==================================================================

    /**
     * Monthly forecast buckets with owner and stage-mix cuts.
     *
     * <p>Window required and half-open {@code [from, to)} so adjacent periods
     * cannot double count a boundary month, matching Module 3's forecast and the
     * Phase 3 reports.
     *
     * <p>{@code pipelineCoverage} is the sum of the probability weights of stages
     * that currently hold open value. It is a health read, not a sum: stages that
     * can never close (weight 0) make coverage fall below 100%, which is the
     * signal that part of the forecast is unreachable.
     */
    @Transactional(readOnly = true)
    public ForecastReportResponse forecastReport(UUID ownerId,
                                                 LocalDate from,
                                                 LocalDate to,
                                                 UserPrincipal caller) {
        requireWindow(from, to);
        UUID effectiveOwner = SEES_ALL.contains(caller.role()) ? ownerId : caller.id();

        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("from", from);
        p.addValue("to", to);
        StringBuilder scope = new StringBuilder();
        if (effectiveOwner != null) {
            scope.append(" and o.owner_id = :ownerId");
            p.addValue("ownerId", effectiveOwner);
        }

        String rowsSql = """
                select o.id, o.expected_value, o.expected_date, o.status,
                       s.stage_key, s.label, s.probability_weight,
                       u.id as owner_id, u.full_name
                  from opportunities o
                  join pipeline_stages s on s.id = o.stage_id
                  join users u on u.id = o.owner_id
                 where o.expected_date >= :from and o.expected_date < :to %s
                """.formatted(scope);

        List<Map<String, Object>> rows = jdbc.queryForList(rowsSql, p);

        Map<String, MonthAcc> months = new LinkedHashMap<>();
        Map<UUID, OwnerAcc> owners = new LinkedHashMap<>();
        Map<String, MixAcc> mix = new LinkedHashMap<>();

        BigDecimal expected = money(0);
        BigDecimal best = money(0);
        BigDecimal won = money(0);
        long openInWindow = 0;
        long wonCount = 0;

        for (Map<String, Object> r : rows) {
            String status = (String) r.get("status");
            BigDecimal value = money(r.get("expected_value"));
            String month = String.valueOf(r.get("expected_date")).substring(0, 7);
            String stageKey = (String) r.get("stage_key");
            BigDecimal weight = money(r.get("probability_weight"));
            UUID ownerKey = (UUID) r.get("owner_id");
            OwnerAcc owner = owners.computeIfAbsent(ownerKey, k -> new OwnerAcc(k, (String) r.get("full_name")));
            MixAcc stage = mix.computeIfAbsent(stageKey,
                    k -> new MixAcc(k, (String) r.get("label"), weight));
            MonthAcc bucket = months.computeIfAbsent(month, k -> new MonthAcc());

            if ("WON".equals(status)) {
                won = won.add(value);
                wonCount++;
                bucket.wonCount++;
                bucket.won = bucket.won.add(value);
                owner.wonCount++;
                owner.won = owner.won.add(value);
            } else if ("OPEN".equals(status)) {
                BigDecimal weighted = money(value.multiply(weight).divide(HUNDRED, 2, RoundingMode.HALF_UP));
                openInWindow++;
                expected = expected.add(weighted);
                best = best.add(value);

                bucket.openCount++;
                bucket.expected = bucket.expected.add(weighted);
                bucket.best = bucket.best.add(value);

                owner.openCount++;
                owner.openValue = owner.openValue.add(value);
                owner.expected = owner.expected.add(weighted);

                stage.openCount++;
                stage.openValue = stage.openValue.add(value);
                stage.expected = stage.expected.add(weighted);
            }
            // LOST is a terminal outcome, not pipeline: counting it would make a
            // lost deal look like forecast.
        }

        List<ForecastReportResponse.Month> monthRows = months.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ForecastReportResponse.Month(e.getKey(), e.getValue().openCount,
                        e.getValue().expected, e.getValue().best, e.getValue().wonCount, e.getValue().won))
                .toList();

        List<ForecastReportResponse.Owner> ownerRows = new ArrayList<>();
        for (OwnerAcc o : owners.values()) {
            ownerRows.add(new ForecastReportResponse.Owner(o.id, o.name, o.openCount, o.openValue,
                    o.expected, o.wonCount, o.won, pctOfExpected(o.expected, expected)));
        }
        ownerRows.sort((a, b) -> b.expected().compareTo(a.expected()));

        List<ForecastReportResponse.StageMix> mixRows = new ArrayList<>();
        BigDecimal coveredWeight = BigDecimal.ZERO;
        for (MixAcc m : mix.values()) {
            if (m.openCount == 0) {
                continue;   // a stage with no open value contributes no mix
            }
            coveredWeight = coveredWeight.add(m.weight);
            mixRows.add(new ForecastReportResponse.StageMix(m.key, m.label, m.weight, m.openCount,
                    m.openValue, m.expected, pctOfExpected(m.expected, expected)));
        }
        mixRows.sort((a, b) -> b.expected().compareTo(a.expected()));

        String outsideSql = """
                select count(*) as n from opportunities o
                 where o.status = 'OPEN'
                   and (o.expected_date < :from or o.expected_date >= :to) %s
                """.formatted(scope);
        long openOutsideWindow = asLong(jdbc.queryForMap(outsideSql, p).get("n"));

        ForecastReportResponse.ValueBasis basis = new ForecastReportResponse.ValueBasis(
                "expected = sum(open expected_value * stage probability_weight / 100), HALF_UP at scale 2",
                "best = sum(open expected_value) in the half-open window [from, to)",
                "won = sum(expected_value of WON opportunities closed in [from, to))",
                true, true);

        return new ForecastReportResponse(scopeLabel(caller), effectiveOwner, from, to, basis,
                monthRows, ownerRows, mixRows,
                new ForecastReportResponse.Totals(openInWindow, openOutsideWindow, best, expected, best,
                        wonCount, won, coverage(coveredWeight)));
    }

    /**
     * Weighted share of stages holding open value, capped at 100.
     *
     * <p>Capped because the stages in the mix are distinct rows, but a report
     * that could show 160% would be read as an error in the data rather than as
     * the sum of stage weights it is.
     */
    private static BigDecimal coverage(BigDecimal coveredWeight) {
        BigDecimal pct = coveredWeight.min(HUNDRED).setScale(1, RoundingMode.HALF_UP);
        return pct;
    }

    // ==================================================================
    // 3. Partner commission report
    // ==================================================================

    /**
     * Per-account and per-plan partner commission.
     *
     * <p>Window on {@code payable_at}, half-open. Payables with
     * {@code plan_id IS NULL} predate Module 4 or came from the flat fallback;
     * they are grouped under the synthetic key {@code DEFAULT_FLAT_RATE} so a
     * per-plan rollup stays complete when an account moves onto a plan.
     *
     * <p>{@code effectiveRate} is the rate of the most recent payable in the
     * window, so it reads as "what this account is currently being paid at"
     * without pretending to be the plan's current terms (a plan may have been
     * re-assigned after the accrual).
     */
    @Transactional(readOnly = true)
    public PartnerCommissionReportResponse partnerCommissionReport(LocalDate from,
                                                                 LocalDate to,
                                                                 UUID accountId,
                                                                 UserPrincipal caller) {
        if (!MANAGER_AND_UP.contains(caller.role())) {
            throw new ForbiddenException("Partner commission reporting is restricted to managers");
        }
        requireWindow(from, to);

        MapSqlParameterSource p = new MapSqlParameterSource();
        p.addValue("fromTs", Timestamp.from(from.atStartOfDay(ZONE).toInstant()));
        p.addValue("toTs", Timestamp.from(to.atStartOfDay(ZONE).toInstant()));
        StringBuilder scope = new StringBuilder();
        if (accountId != null) {
            scope.append(" and cp.account_id = :accountId");
            p.addValue("accountId", accountId);
        }

        String accountSql = """
                select cp.account_id                                   as account_id,
                       a.name                                           as account_name,
                       cp.plan_id                                       as plan_id,
                       coalesce(pl.plan_key, 'DEFAULT_FLAT_RATE')        as plan_key,
                       coalesce(pl.basis, cp.basis)                     as plan_basis,
                       count(*)                                         as payables,
                       coalesce(sum(cp.commission_amount), 0)            as accrued,
                       coalesce(sum(cp.commission_amount)
                           filter (where cp.status = 'PAID'), 0)        as paid,
                       coalesce(sum(cp.commission_amount)
                           filter (where cp.status = 'OPEN'), 0)        as open_liability,
                       count(*) filter (where cp.status = 'VOID')       as voided_count,
                       coalesce(sum(cp.commission_amount)
                           filter (where cp.status = 'VOID'), 0)        as voided_amount,
                       (array_agg(cp.rate_percent
                            order by cp.payable_at desc))[1]            as effective_rate
                  from account_commission_payables cp
                  join accounts a on a.id = cp.account_id
             left join commission_plans pl on pl.id = cp.plan_id
                 where cp.payable_at >= :fromTs and cp.payable_at < :toTs %s
                 group by cp.account_id, a.name, cp.plan_id, cp.basis, pl.plan_key, pl.basis
                 order by open_liability desc, accrued desc, a.name
                """.formatted(scope);

        List<Map<String, Object>> accountRows = jdbc.queryForList(accountSql, p);

        List<PartnerCommissionReportResponse.Account> accounts = new ArrayList<>();
        for (Map<String, Object> r : accountRows) {
            Object rate = r.get("effective_rate");
            accounts.add(new PartnerCommissionReportResponse.Account(
                    (UUID) r.get("account_id"),
                    (String) r.get("account_name"),
                    (UUID) r.get("plan_id"),
                    (String) r.get("plan_key"),
                    (String) r.get("plan_basis"),
                    asLong(r.get("payables")),
                    money(r.get("accrued")),
                    money(r.get("paid")),
                    money(r.get("open_liability")),
                    asLong(r.get("voided_count")),
                    money(r.get("voided_amount")),
                    rate == null ? null : money(rate).toPlainString() + "%",
                    rate == null ? null : money(rate)));
        }

        String planSql = """
                select cp.plan_id                                       as plan_id,
                       coalesce(pl.plan_key, 'DEFAULT_FLAT_RATE')        as plan_key,
                       coalesce(pl.label, 'Flat default rate (no plan assigned)') as label,
                       coalesce(pl.method, 'PERCENT')                    as method,
                       coalesce(pl.basis, 'NET')                         as basis,
                       count(distinct cp.account_id) filter (where acp.account_id is not null) as active_accounts,
                       count(*)                                         as payables,
                       coalesce(sum(cp.commission_amount), 0)            as accrued,
                       coalesce(sum(cp.commission_amount)
                           filter (where cp.status = 'PAID'), 0)        as paid,
                       coalesce(sum(cp.commission_amount)
                           filter (where cp.status = 'OPEN'), 0)        as open_liability
                  from account_commission_payables cp
             left join commission_plans pl on pl.id = cp.plan_id
             left join account_commission_plans acp
                    on acp.plan_id = cp.plan_id and acp.is_active
                 where cp.payable_at >= :fromTs and cp.payable_at < :toTs %s
                 group by cp.plan_id, pl.plan_key, pl.label, pl.method, pl.basis
                 order by open_liability desc, accrued desc, plan_key
                """.formatted(scope);

        List<PartnerCommissionReportResponse.Plan> plans = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(planSql, p)) {
            plans.add(new PartnerCommissionReportResponse.Plan(
                    (UUID) r.get("plan_id"),
                    (String) r.get("plan_key"),
                    (String) r.get("label"),
                    (String) r.get("method"),
                    (String) r.get("basis"),
                    asLong(r.get("active_accounts")),
                    asLong(r.get("payables")),
                    money(r.get("accrued")),
                    money(r.get("paid")),
                    money(r.get("open_liability"))));
        }

        long totalPayables = 0;
        BigDecimal accrued = money(0);
        BigDecimal paid = money(0);
        BigDecimal open = money(0);
        BigDecimal voided = money(0);
        // Rows are grouped by account AND plan, so one account that changed plans
        // mid-history contributes more than one row. Counting rows would report
        // that single account twice in a field that says "accounts"; the distinct
        // set is the number of partners the money actually went to.
        java.util.Set<UUID> distinctAccounts = new java.util.HashSet<>();
        for (PartnerCommissionReportResponse.Account a : accounts) {
            totalPayables += a.payables();
            accrued = accrued.add(a.accrued());
            paid = paid.add(a.paid());
            open = open.add(a.openLiability());
            voided = voided.add(a.voidedAmount());
            distinctAccounts.add(a.accountId());
        }

        PartnerCommissionReportResponse.Totals totals = new PartnerCommissionReportResponse.Totals(
                distinctAccounts.size(), totalPayables, accrued, paid, open, voided,
                pctOfExpected(paid, accrued),
                totalPayables == 0 ? money(0)
                        : accrued.divide(BigDecimal.valueOf(totalPayables), 2, RoundingMode.HALF_UP));

        return new PartnerCommissionReportResponse(from, to,
                "payable_at in the half-open window [from, to)", accounts, plans, totals);
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private static void requireWindow(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("from and to are required");
        }
        if (!from.isBefore(to)) {
            throw new BadRequestException("Window must satisfy from < to");
        }
    }

    /**
     * Pipeline health is a standing question about the book as it is, so its
     * window is optional: both bounds absent means the whole pipeline, and a
     * half-specified window is a client bug worth naming rather than silently
     * widening to everything. The forecast and commission reports keep
     * {@link #requireWindow} because an all-time horizon is meaningless for
     * either.
     */
    private static void validateOptionalWindow(LocalDate from, LocalDate to) {
        if ((from == null) != (to == null)) {
            throw new BadRequestException("from and to must be supplied together, or both omitted");
        }
        if (from != null) {
            requireWindow(from, to);
        }
    }

    private static String scopeLabel(UserPrincipal caller) {
        return SEES_ALL.contains(caller.role()) ? "ALL" : "SELF";
    }

    private static BigDecimal money(Object o) {
        if (o == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal b = o instanceof BigDecimal d ? d : new BigDecimal(o.toString());
        return b.setScale(2, RoundingMode.HALF_UP);
    }

    private static long asLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(o.toString());
    }

    private static double asDouble(Object o) {
        if (o == null) return 0d;
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(o.toString());
    }

    /** Null when there is no denominator: "0 of 0" is not 0%. */
    private static BigDecimal pct(long part, long whole) {
        if (whole == 0) return null;
        return BigDecimal.valueOf(part).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal pctOfExpected(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) return null;
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP);
    }

    private static final class MonthAcc {
        long openCount;
        BigDecimal expected = BigDecimal.ZERO;
        BigDecimal best = BigDecimal.ZERO;
        long wonCount;
        BigDecimal won = BigDecimal.ZERO;
    }

    private static final class OwnerAcc {
        final UUID id;
        final String name;
        long openCount;
        BigDecimal openValue = BigDecimal.ZERO;
        BigDecimal expected = BigDecimal.ZERO;
        long wonCount;
        BigDecimal won = BigDecimal.ZERO;

        OwnerAcc(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final class MixAcc {
        final String key;
        final String label;
        final BigDecimal weight;
        long openCount;
        BigDecimal openValue = BigDecimal.ZERO;
        BigDecimal expected = BigDecimal.ZERO;

        MixAcc(String key, String label, BigDecimal weight) {
            this.key = key;
            this.label = label;
            this.weight = weight;
        }
    }
}
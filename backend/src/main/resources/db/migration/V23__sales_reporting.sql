-- Phase 7 Module 5 - read support for the pipeline, forecast and partner
-- commission reports.
--
-- Indexes only, deliberately. Module 3 already established that a report worth
-- recomputing is worth recomputing live: re-weighting a stage must change every
-- forecast immediately, so a rollup table would be a second source of truth that
-- can only drift. These indexes make the live aggregate queries index-only
-- where possible; they add no columns and no pre-computed numbers.
--
-- Nothing here is referenced by a foreign key or by the write path, so this
-- migration is safe to apply to a live database.

-- Pipeline report: GROUP BY stage over the caller's scope, splitting
-- OPEN / WON / LOST. V21 already indexes stage_id and (owner_id, status);
-- (stage_id, status) lets one stage's three counts share a single index scan.
CREATE INDEX idx_opportunities_stage_status
    ON opportunities(stage_id, status);

-- Forecast report: month buckets over [from, to) on expected_date, filtered to
-- OPEN vs WON in the same scan.
CREATE INDEX idx_opportunities_expected_status
    ON opportunities(expected_date, status);

-- Staleness ("no movement in N days") reads stage_moved_at per stage.
CREATE INDEX idx_opportunities_stage_moved
    ON opportunities(stage_id, stage_moved_at);

-- Partner commission report: time-windowed sums over payable_at, which V19's
-- (account_id, status) and (status) indexes cannot serve.
CREATE INDEX idx_commission_payables_payable_at
    ON account_commission_payables(payable_at);

-- Settled/partners-by-plan drill-down keyed on the plan.
CREATE INDEX idx_commission_payables_plan_status
    ON account_commission_payables(plan_id, status);
-- =====================================================================
-- SecureTravels CRM — Phase 7, Module 4 (Commission plans)
-- Configurable partner-commission terms for travel-agent accounts,
-- replacing the single flat default rate. A plan states the basis it
-- is computed on (NET/GROSS), the method (PERCENT/FIXED/TIERED) and
-- an optional minimum-sales threshold below which no commission is
-- owed. TIERED plans carry boundary-aware bands in commission_tiers.
--
-- The calculation runs at BookingService.confirm beside — and never
-- in place of — the sales-consultant credit in sales_commission_ledger:
-- that ledger's immutability contract is untouched.
-- =====================================================================

-- 1) Plan header. key is the immutable identity (plans are referenced
--    by key in reports and audit trails).
CREATE TABLE commission_plans (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_key           varchar(40) NOT NULL UNIQUE,
    label              varchar(120) NOT NULL,
    basis              varchar(10) NOT NULL DEFAULT 'NET'
        CHECK (basis IN ('NET', 'GROSS')),
    method             varchar(10) NOT NULL
        CHECK (method IN ('PERCENT', 'FIXED', 'TIERED')),
    rate_percent       numeric(5,2)
        CHECK (rate_percent IS NULL OR (rate_percent >= 0 AND rate_percent <= 100)),
    fixed_amount       numeric(12,2)
        CHECK (fixed_amount IS NULL OR fixed_amount >= 0),
    min_sales_threshold numeric(12,2) NOT NULL DEFAULT 0
        CHECK (min_sales_threshold >= 0),
    is_active          boolean NOT NULL DEFAULT true,
    notes              text,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    version            bigint NOT NULL DEFAULT 0,
    -- Each method needs its own input and must not carry a foreign one:
    -- PERCENT needs a rate, FIXED needs an amount, TIERED reads neither
    -- (its rates live in commission_tiers).
    CONSTRAINT chk_commission_plan_method_inputs CHECK (
        (method = 'PERCENT' AND rate_percent IS NOT NULL AND fixed_amount IS NULL)
        OR (method = 'FIXED'   AND fixed_amount IS NOT NULL AND rate_percent IS NULL)
        OR (method = 'TIERED'  AND rate_percent IS NULL     AND fixed_amount IS NULL)
    )
);
CREATE INDEX idx_commission_plans_active ON commission_plans(is_active);

-- 2) Tiers for TIERED plans. Bands are half-open [from_amount, to_amount)
--    so adjacent tiers never both claim the same booking value: an amount
--    exactly equal to a boundary belongs to the upper tier. A NULL
--    to_amount is the open-ended top band.
CREATE TABLE commission_tiers (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id      uuid NOT NULL REFERENCES commission_plans(id) ON DELETE CASCADE,
    from_amount  numeric(12,2) NOT NULL CHECK (from_amount >= 0),
    to_amount    numeric(12,2) CHECK (to_amount IS NULL OR to_amount > from_amount),
    rate_percent numeric(5,2) NOT NULL
        CHECK (rate_percent >= 0 AND rate_percent <= 100),
    created_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_commission_tiers_plan ON commission_tiers(plan_id, from_amount);

-- 3) Plan assignment: which plan an account is served by. One active
--    assignment per account (partial unique index) so resolution is
--    unambiguous; superseded rows are retained for the audit trail.
CREATE TABLE account_commission_plans (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id  uuid NOT NULL REFERENCES accounts(id),
    plan_id     uuid NOT NULL REFERENCES commission_plans(id),
    assigned_at timestamptz NOT NULL DEFAULT now(),
    assigned_by uuid REFERENCES users(id),
    is_active   boolean NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX idx_account_commission_plans_active
    ON account_commission_plans(account_id) WHERE is_active;
CREATE INDEX idx_account_commission_plans_plan ON account_commission_plans(plan_id);

-- 4) Record which plan produced a payable, so a settled amount can be
--    traced back to the terms in force at confirmation time. Nullable:
--    payables written before Module 4 used the flat default rate.
ALTER TABLE account_commission_payables
    ADD COLUMN plan_id uuid REFERENCES commission_plans(id);
CREATE INDEX idx_commission_payables_plan ON account_commission_payables(plan_id);

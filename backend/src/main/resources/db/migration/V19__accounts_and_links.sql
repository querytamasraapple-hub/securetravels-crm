-- =====================================================================
-- SecureTravels CRM — Phase 7, Module 1 (Account records)
-- B2B/corporate + travel-agent accounts, their links onto leads and
-- bookings, the travel-agent commission payable, and account-first
-- invoices. Full payables/invoicing depth stays Phase 9; this ships the
-- account shapes with regression-safe retail behaviour.
-- =====================================================================

-- 1) Accounts master: a legal entity (corporate) or travel agent that
--    books through SecureTravels. Primary contact fields are PII and are
--    sanitised on write like customer data.
CREATE TABLE accounts (
    id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_type             varchar(20) NOT NULL
        CHECK (account_type IN ('CORPORATE', 'TRAVEL_AGENT')),
    name                     varchar(200) NOT NULL,
    gstin                    varchar(15),
    billing_name             varchar(200),
    billing_address          text,
    city                     varchar(80),
    primary_contact_name     varchar(200),
    primary_contact_email    varchar(255),
    primary_contact_phone    varchar(30),
    bank_account_ref         varchar(40),
    credit_terms_days        int CHECK (credit_terms_days IS NULL OR credit_terms_days >= 0),
    notes                    text,
    is_active                boolean NOT NULL DEFAULT true,
    created_at               timestamptz NOT NULL DEFAULT now(),
    updated_at               timestamptz NOT NULL DEFAULT now(),
    version                  bigint NOT NULL DEFAULT 0
);
CREATE INDEX idx_accounts_type_active ON accounts(account_type, is_active);
CREATE INDEX idx_accounts_gstin ON accounts(gstin);

-- 2) Links: a lead may belong to an account; a booking inherits it.
ALTER TABLE leads ADD COLUMN account_id uuid REFERENCES accounts(id);
CREATE INDEX idx_leads_account ON leads(account_id);

ALTER TABLE bookings ADD COLUMN account_id uuid REFERENCES accounts(id);
CREATE INDEX idx_bookings_account ON bookings(account_id);

-- 3) Travel-agent commission payable: what SecureTravels owes an account
--    on a confirmed booking. One row per booking — the unique booking_id
--    is the idempotency guard, mirroring sales_commission_ledger. A
--    credit is never deleted; it is VOIDed with a retained reason.
CREATE TABLE account_commission_payables (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id         uuid NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    account_id         uuid NOT NULL REFERENCES accounts(id),
    payable_at         timestamptz NOT NULL DEFAULT now(),
    basis              varchar(10) NOT NULL DEFAULT 'NET'
        CHECK (basis IN ('NET', 'GROSS')),
    rate_percent       numeric(6,2) NOT NULL,
    gross_amount       numeric(12,2) NOT NULL DEFAULT 0,
    discount_amount    numeric(12,2) NOT NULL DEFAULT 0,
    tax_amount         numeric(12,2) NOT NULL DEFAULT 0,
    net_amount         numeric(12,2) NOT NULL,
    commission_amount  numeric(12,2) NOT NULL,
    status             varchar(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'PAID', 'VOID')),
    paid_at            timestamptz,
    paid_ref           varchar(120),
    settled_by         uuid REFERENCES users(id),
    notes              text,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    version            bigint NOT NULL DEFAULT 0
);
CREATE INDEX idx_commission_payables_account_status
    ON account_commission_payables(account_id, status);
CREATE INDEX idx_commission_payables_status
    ON account_commission_payables(status);

-- 4) Account-first invoice: an invoice row is created when a CONFIRMED
--    booking carries an account, billed to that account with its billing
--    name/address/GSTIN. Retail bookings generate no invoice (unchanged
--    Phase 1 behaviour). Net = gross - discount + tax, matching the
--    sales_commission_ledger definition. VOID is the only reversal.
CREATE TABLE invoices (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_ref    varchar(20) NOT NULL UNIQUE,
    booking_id     uuid NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    account_id     uuid REFERENCES accounts(id),
    billing_entity varchar(20) NOT NULL
        CHECK (billing_entity IN ('ACCOUNT', 'RETAIL')),
    billing_name   varchar(200) NOT NULL,
    billing_address text,
    billing_gstin  varchar(15),
    gross_amount   numeric(12,2) NOT NULL DEFAULT 0,
    discount_amount numeric(12,2) NOT NULL DEFAULT 0,
    tax_amount     numeric(12,2) NOT NULL DEFAULT 0,
    net_amount     numeric(12,2) NOT NULL,
    status         varchar(20) NOT NULL DEFAULT 'ISSUED'
        CHECK (status IN ('ISSUED', 'VOID')),
    issued_at      timestamptz NOT NULL DEFAULT now(),
    issued_by      uuid REFERENCES users(id),
    notes          text,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    version        bigint NOT NULL DEFAULT 0
);
CREATE INDEX idx_invoices_account ON invoices(account_id);
CREATE INDEX idx_invoices_status ON invoices(status);
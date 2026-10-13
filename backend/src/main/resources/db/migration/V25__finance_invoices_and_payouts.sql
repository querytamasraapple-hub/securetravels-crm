-- Finance ERP depth: customer invoices + vendor / guide payouts.

CREATE TABLE finance_invoices (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_no      varchar(40)  NOT NULL UNIQUE,
    booking_id      uuid REFERENCES bookings(id) ON DELETE SET NULL,
    customer_name   varchar(160) NOT NULL,
    customer_email  varchar(255),
    issue_date      date         NOT NULL,
    due_date        date         NOT NULL,
    subtotal        numeric(12, 2) NOT NULL CHECK (subtotal >= 0),
    tax_amount      numeric(12, 2) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    total_amount    numeric(12, 2) NOT NULL CHECK (total_amount >= 0),
    currency        varchar(3)   NOT NULL DEFAULT 'INR',
    status          varchar(20)  NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'SENT', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED')),
    notes           text,
    recorded_by     uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    version         bigint       NOT NULL DEFAULT 0
);

CREATE INDEX idx_finance_invoices_status ON finance_invoices (status);
CREATE INDEX idx_finance_invoices_due ON finance_invoices (due_date);

CREATE TABLE finance_vendor_payouts (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_name     varchar(160) NOT NULL,
    category        varchar(40)  NOT NULL
        CHECK (category IN (
            'GUIDE', 'PORTER', 'HOTEL', 'TRANSPORT', 'PERMITS',
            'FOOD', 'GEAR', 'AGENT_COMMISSION', 'OTHER'
        )),
    trip_id         uuid REFERENCES trips(id) ON DELETE SET NULL,
    amount          numeric(12, 2) NOT NULL CHECK (amount >= 0),
    payout_date     date         NOT NULL,
    due_date        date,
    payment_method  varchar(40)  NOT NULL DEFAULT 'BANK_TRANSFER'
        CHECK (payment_method IN ('CASH', 'UPI', 'BANK_TRANSFER', 'CARD', 'CHEQUE')),
    status          varchar(20)  NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN ('SCHEDULED', 'PAID', 'HOLD', 'CANCELLED')),
    reference_no    varchar(80),
    notes           text,
    recorded_by     uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    version         bigint       NOT NULL DEFAULT 0
);

CREATE INDEX idx_finance_payouts_status ON finance_vendor_payouts (status);
CREATE INDEX idx_finance_payouts_date ON finance_vendor_payouts (payout_date DESC);

CREATE TABLE finance_budgets (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    period_month    varchar(7)   NOT NULL,
    category        varchar(40)  NOT NULL,
    budget_amount   numeric(12, 2) NOT NULL CHECK (budget_amount >= 0),
    notes           text,
    recorded_by     uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    version         bigint       NOT NULL DEFAULT 0,
    UNIQUE (period_month, category)
);

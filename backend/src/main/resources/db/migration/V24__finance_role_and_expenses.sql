-- Finance portal: expand staff roles + company expense ledger for trek ops.

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('SALES', 'OPS', 'MANAGER', 'ADMIN', 'CEO', 'FINANCE'));

CREATE TABLE finance_expenses (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category        varchar(40)  NOT NULL
        CHECK (category IN (
            'GUIDE_FEES', 'TRANSPORT', 'HOTEL', 'PERMITS', 'FOOD',
            'GEAR', 'MARKETING', 'SALARIES', 'INSURANCE', 'OTHER'
        )),
    title           varchar(200) NOT NULL,
    amount          numeric(12, 2) NOT NULL CHECK (amount >= 0),
    expense_date    date         NOT NULL,
    trip_id         uuid REFERENCES trips(id) ON DELETE SET NULL,
    vendor_name     varchar(160),
    payment_method  varchar(40)  NOT NULL DEFAULT 'BANK_TRANSFER'
        CHECK (payment_method IN ('CASH', 'UPI', 'BANK_TRANSFER', 'CARD', 'CHEQUE')),
    status          varchar(20)  NOT NULL DEFAULT 'POSTED'
        CHECK (status IN ('DRAFT', 'POSTED', 'VOID')),
    notes           text,
    recorded_by     uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    version         bigint       NOT NULL DEFAULT 0
);

CREATE INDEX idx_finance_expenses_date ON finance_expenses (expense_date DESC);
CREATE INDEX idx_finance_expenses_category ON finance_expenses (category);
CREATE INDEX idx_finance_expenses_trip ON finance_expenses (trip_id);

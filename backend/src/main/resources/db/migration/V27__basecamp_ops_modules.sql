-- Basecamp OS ops modules: attendance, leave, payroll, inventory, visitors, safe settings.

CREATE TABLE staff_attendance (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    work_date       date NOT NULL,
    check_in_at     timestamptz,
    check_out_at    timestamptz,
    status          varchar(20) NOT NULL DEFAULT 'PRESENT'
        CHECK (status IN ('PRESENT', 'ABSENT', 'HALF_DAY', 'ON_LEAVE', 'WFH')),
    notes           text,
    recorded_by     uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint NOT NULL DEFAULT 0,
    UNIQUE (user_id, work_date)
);

CREATE INDEX idx_staff_attendance_date ON staff_attendance (work_date DESC);
CREATE INDEX idx_staff_attendance_user ON staff_attendance (user_id);

CREATE TABLE staff_leave_requests (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    leave_type      varchar(30) NOT NULL
        CHECK (leave_type IN ('CASUAL', 'SICK', 'EARNED', 'UNPAID', 'COMP_OFF')),
    from_date       date NOT NULL,
    to_date         date NOT NULL,
    reason          text,
    status          varchar(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    reviewed_by     uuid REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at     timestamptz,
    review_note     text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint NOT NULL DEFAULT 0,
    CHECK (to_date >= from_date)
);

CREATE INDEX idx_staff_leave_user ON staff_leave_requests (user_id);
CREATE INDEX idx_staff_leave_status ON staff_leave_requests (status);

CREATE TABLE staff_payroll (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_month    varchar(7) NOT NULL,
    basic_pay       numeric(12, 2) NOT NULL DEFAULT 0 CHECK (basic_pay >= 0),
    allowances      numeric(12, 2) NOT NULL DEFAULT 0 CHECK (allowances >= 0),
    deductions      numeric(12, 2) NOT NULL DEFAULT 0 CHECK (deductions >= 0),
    net_pay         numeric(12, 2) NOT NULL DEFAULT 0,
    status          varchar(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'APPROVED', 'PAID', 'VOID')),
    notes           text,
    recorded_by     uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint NOT NULL DEFAULT 0,
    UNIQUE (user_id, period_month)
);

CREATE INDEX idx_staff_payroll_month ON staff_payroll (period_month);

CREATE TABLE inventory_items (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sku             varchar(40) NOT NULL UNIQUE,
    name            varchar(160) NOT NULL,
    category        varchar(40) NOT NULL DEFAULT 'GEAR'
        CHECK (category IN ('GEAR', 'SAFETY', 'CAMPING', 'MEDICAL', 'OFFICE', 'OTHER')),
    quantity        numeric(12, 2) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    unit            varchar(20) NOT NULL DEFAULT 'NOS',
    reorder_level   numeric(12, 2) NOT NULL DEFAULT 0 CHECK (reorder_level >= 0),
    location        varchar(120),
    active          boolean NOT NULL DEFAULT true,
    notes           text,
    recorded_by     uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_inventory_active ON inventory_items (active);

CREATE TABLE visitor_logs (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    visitor_name    varchar(160) NOT NULL,
    phone           varchar(20),
    purpose         varchar(200) NOT NULL,
    host_user_id    uuid REFERENCES users(id) ON DELETE SET NULL,
    checked_in_at   timestamptz NOT NULL DEFAULT now(),
    checked_out_at  timestamptz,
    notes           text,
    recorded_by     uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint NOT NULL DEFAULT 0
);

CREATE INDEX idx_visitor_logs_in ON visitor_logs (checked_in_at DESC);

-- Non-secret company settings only (never JWT / DB passwords via this table).
CREATE TABLE company_settings (
    setting_key     varchar(80) PRIMARY KEY,
    setting_value   text NOT NULL,
    updated_by      uuid REFERENCES users(id) ON DELETE SET NULL,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint NOT NULL DEFAULT 0
);

INSERT INTO company_settings (setting_key, setting_value) VALUES
    ('company.legal_name', 'Trip On Himalaya Adventures'),
    ('company.trade_name', 'Trip On Himalaya'),
    ('company.gstin', '05AABCT0000A1Z5'),
    ('company.support_email', 'hello@triponhimalaya.com'),
    ('company.support_phone', '9876000000'),
    ('company.timezone', 'Asia/Kolkata')
ON CONFLICT DO NOTHING;

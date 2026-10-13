-- Upgrade finance invoices to GST tax-invoice shape + WhatsApp automation fields.

ALTER TABLE finance_invoices
    ADD COLUMN IF NOT EXISTS customer_whatsapp varchar(20),
    ADD COLUMN IF NOT EXISTS customer_phone varchar(20),
    ADD COLUMN IF NOT EXISTS customer_gstin varchar(20),
    ADD COLUMN IF NOT EXISTS customer_address text,
    ADD COLUMN IF NOT EXISTS customer_state varchar(80),
    ADD COLUMN IF NOT EXISTS customer_state_code varchar(2),
    ADD COLUMN IF NOT EXISTS place_of_supply varchar(80),
    ADD COLUMN IF NOT EXISTS place_of_supply_code varchar(2),
    ADD COLUMN IF NOT EXISTS seller_legal_name varchar(200),
    ADD COLUMN IF NOT EXISTS seller_trade_name varchar(200),
    ADD COLUMN IF NOT EXISTS seller_gstin varchar(20),
    ADD COLUMN IF NOT EXISTS seller_address text,
    ADD COLUMN IF NOT EXISTS seller_state varchar(80),
    ADD COLUMN IF NOT EXISTS seller_state_code varchar(2),
    ADD COLUMN IF NOT EXISTS seller_phone varchar(20),
    ADD COLUMN IF NOT EXISTS seller_email varchar(255),
    ADD COLUMN IF NOT EXISTS seller_bank_name varchar(120),
    ADD COLUMN IF NOT EXISTS seller_bank_account varchar(40),
    ADD COLUMN IF NOT EXISTS seller_bank_ifsc varchar(20),
    ADD COLUMN IF NOT EXISTS invoice_type varchar(30) NOT NULL DEFAULT 'TAX_INVOICE',
    ADD COLUMN IF NOT EXISTS supply_type varchar(20) NOT NULL DEFAULT 'INTRA',
    ADD COLUMN IF NOT EXISTS reverse_charge boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS sac_code varchar(12) NOT NULL DEFAULT '998555',
    ADD COLUMN IF NOT EXISTS cgst_rate numeric(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sgst_rate numeric(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS igst_rate numeric(5, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cgst_amount numeric(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sgst_amount numeric(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS igst_amount numeric(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS amount_in_words varchar(500),
    ADD COLUMN IF NOT EXISTS declaration text,
    ADD COLUMN IF NOT EXISTS whatsapp_delivery_status varchar(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS whatsapp_queued_at timestamptz,
    ADD COLUMN IF NOT EXISTS whatsapp_sent_at timestamptz,
    ADD COLUMN IF NOT EXISTS whatsapp_last_error text;

ALTER TABLE finance_invoices DROP CONSTRAINT IF EXISTS finance_invoices_invoice_type_check;
ALTER TABLE finance_invoices ADD CONSTRAINT finance_invoices_invoice_type_check
    CHECK (invoice_type IN ('TAX_INVOICE', 'BILL_OF_SUPPLY', 'PROFORMA'));

ALTER TABLE finance_invoices DROP CONSTRAINT IF EXISTS finance_invoices_supply_type_check;
ALTER TABLE finance_invoices ADD CONSTRAINT finance_invoices_supply_type_check
    CHECK (supply_type IN ('INTRA', 'INTER'));

ALTER TABLE finance_invoices DROP CONSTRAINT IF EXISTS finance_invoices_whatsapp_status_check;
ALTER TABLE finance_invoices ADD CONSTRAINT finance_invoices_whatsapp_status_check
    CHECK (whatsapp_delivery_status IN ('PENDING', 'QUEUED', 'SENT', 'FAILED', 'SKIPPED'));

CREATE INDEX IF NOT EXISTS idx_finance_invoices_whatsapp ON finance_invoices (customer_whatsapp);
CREATE INDEX IF NOT EXISTS idx_finance_invoices_wa_status ON finance_invoices (whatsapp_delivery_status);

CREATE TABLE IF NOT EXISTS finance_invoice_lines (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id      uuid NOT NULL REFERENCES finance_invoices(id) ON DELETE CASCADE,
    line_no         int  NOT NULL,
    description     varchar(400) NOT NULL,
    hsn_sac         varchar(12) NOT NULL DEFAULT '998555',
    quantity        numeric(12, 2) NOT NULL DEFAULT 1,
    unit            varchar(20) NOT NULL DEFAULT 'NOS',
    rate            numeric(12, 2) NOT NULL,
    taxable_amount  numeric(12, 2) NOT NULL,
    gst_rate        numeric(5, 2) NOT NULL DEFAULT 5.00,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    version         bigint NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_finance_invoice_lines_invoice ON finance_invoice_lines (invoice_id);

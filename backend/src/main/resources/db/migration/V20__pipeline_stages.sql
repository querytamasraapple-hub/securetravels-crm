-- =====================================================================
-- SecureTravels CRM — Phase 7, Module 2 (Pipeline stage configuration)
-- Configurable opportunity pipeline stages. A stage can be a sales team
-- custom stage or a seeded default; the weights are the kickoff defaults
-- (Qualified 20%, Quotation Sent 40%, Negotiation 70%). Opportunity
-- rows that reference stages arrive with Module 3.
-- =====================================================================

CREATE TABLE pipeline_stages (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    stage_key          varchar(40) NOT NULL UNIQUE,
    label              varchar(80) NOT NULL,
    sort_order         int NOT NULL CHECK (sort_order >= 0),
    probability_weight numeric(5,2) NOT NULL
        CHECK (probability_weight >= 0 AND probability_weight <= 100),
    entry_condition    text,
    is_active          boolean NOT NULL DEFAULT true,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    version            bigint NOT NULL DEFAULT 0
);

-- One ordering slot per active stage: two simultaneous stages cannot share a
-- sort position, so the list order stays deterministic. Inactive stages do
-- not participate (they are not displayed).
CREATE UNIQUE INDEX idx_pipeline_stages_active_sort
    ON pipeline_stages(sort_order) WHERE is_active;

CREATE INDEX idx_pipeline_stages_list ON pipeline_stages(is_active, sort_order);

-- Seeded defaults (kickoff weights). Rows are reference data an admin may
-- edit; fixed ids keep the seed idempotently identifiable across environments.
INSERT INTO pipeline_stages (id, stage_key, label, sort_order, probability_weight, entry_condition) VALUES
    ('11111111-1111-4111-8111-111111111201', 'QUALIFIED',        'Qualified',        10, 20, 'Qualified inbound / B2B lead'),
    ('11111111-1111-4111-8111-111111111202', 'QUOTATION_SENT',   'Quotation Sent',   20, 40, 'Quotation delivered to the prospect'),
    ('11111111-1111-4111-8111-111111111203', 'NEGOTIATION',      'Negotiation',      30, 70, 'Active negotiation on price or dates');
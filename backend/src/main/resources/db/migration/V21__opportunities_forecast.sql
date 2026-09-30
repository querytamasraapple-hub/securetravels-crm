-- =====================================================================
-- SecureTravels CRM — Phase 7, Module 3 (Opportunities + revenue forecast)
-- An opportunity is the sales-deal view of a lead, matched to a configured
-- pipeline stage (V20). Expected-value weighting is applied at read time
-- against the live stage probability, so forecast numbers always reflect
-- the current stage configuration. Materialisation was not needed for the
-- Module 3 forecast; the V-suffix "rollup columns" stay read-model only
-- if Module 5 measurement shows they are required (PHASE_7_DELTA.md §3.3).
-- =====================================================================

CREATE TABLE opportunities (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id           uuid NOT NULL UNIQUE REFERENCES leads(id),
    account_id        uuid REFERENCES accounts(id),
    stage_id          uuid NOT NULL REFERENCES pipeline_stages(id),
    owner_id          uuid NOT NULL REFERENCES users(id),
    expected_value    numeric(12,2) NOT NULL CHECK (expected_value >= 0),
    expected_date     date NOT NULL,
    status            varchar(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'WON', 'LOST')),
    stage_moved_at    timestamptz NOT NULL DEFAULT now(),
    closed_at         timestamptz,
    closing_note      varchar(2000),
    closed_by         uuid REFERENCES users(id),
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    version           bigint NOT NULL DEFAULT 0,
    -- An OPEN deal is not closed; a closed deal has a closer and a timestamp.
    CONSTRAINT chk_opportunity_close_consistency CHECK (
        (status = 'OPEN'  AND closed_at IS NULL    AND closed_by IS NULL)
        OR
        (status <> 'OPEN' AND closed_at IS NOT NULL AND closed_by IS NOT NULL)
    )
);
CREATE INDEX idx_opportunities_owner_status ON opportunities(owner_id, status);
CREATE INDEX idx_opportunities_stage ON opportunities(stage_id);
CREATE INDEX idx_opportunities_expected_date ON opportunities(expected_date);
CREATE INDEX idx_opportunities_lead ON opportunities(lead_id);
package com.securetravels.crm.accounts;

import com.securetravels.crm.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Phase 7 Module 2 — a configurable opportunity pipeline stage. Stages are
 * reference data (seeded with the kickoff defaults) that an administrator may
 * edit: label, sort position, probability weight (0–100, the expected-value
 * basis for Module 3 forecasts), and an entry-condition note. The DB-level
 * partial unique index on active {@code sort_order} keeps list order
 * deterministic and is enforced here with a user-friendly conflict.
 */
@Entity
@Table(name = "pipeline_stages", indexes = {
        @Index(name = "idx_pipeline_stages_list", columnList = "is_active, sort_order")
})
public class PipelineStage extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "stage_key", nullable = false, length = 40, unique = true)
    private String stageKey;

    @Column(name = "label", nullable = false, length = 80)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "probability_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal probabilityWeight;

    @Column(name = "entry_condition")
    private String entryCondition;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public UUID getId() { return id; }
    public String getStageKey() { return stageKey; }
    public void setStageKey(String stageKey) { this.stageKey = stageKey; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public BigDecimal getProbabilityWeight() { return probabilityWeight; }
    public void setProbabilityWeight(BigDecimal probabilityWeight) { this.probabilityWeight = probabilityWeight; }
    public String getEntryCondition() { return entryCondition; }
    public void setEntryCondition(String entryCondition) { this.entryCondition = entryCondition; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
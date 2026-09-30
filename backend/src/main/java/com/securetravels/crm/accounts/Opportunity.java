package com.securetravels.crm.accounts;

import com.securetravels.crm.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Phase 7 Module 3 — an opportunity is the sales-deal view of a lead, sitting
 * on a configured {@link PipelineStage}. It snapshots the lead's owner at
 * creation (the lead's owner is mutable, so Opportunity ownership stays
 * stable), inherits the lead's account, and carries the expected value used
 * by the revenue forecast. Terminal states are {@code WON}/{@code LOST} and
 * are irreversible; DB CHECK enforces the close-consistency invariant.
 */
@Entity
@Table(name = "opportunities", indexes = {
        @Index(name = "idx_opportunities_owner_status", columnList = "owner_id, status"),
        @Index(name = "idx_opportunities_expected_date", columnList = "expected_date")
})
public class Opportunity extends Auditable {

    public enum Status { OPEN, WON, LOST }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lead_id", nullable = false, unique = true)
    private UUID leadId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "stage_id", nullable = false)
    private UUID stageId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "expected_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedValue;

    @Column(name = "expected_date", nullable = false)
    private LocalDate expectedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "stage_moved_at", nullable = false)
    private Instant stageMovedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closing_note")
    private String closingNote;

    @Column(name = "closed_by")
    private UUID closedBy;

    public UUID getId() { return id; }
    public UUID getLeadId() { return leadId; }
    public void setLeadId(UUID leadId) { this.leadId = leadId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getStageId() { return stageId; }
    public void setStageId(UUID stageId) { this.stageId = stageId; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public BigDecimal getExpectedValue() { return expectedValue; }
    public void setExpectedValue(BigDecimal expectedValue) { this.expectedValue = expectedValue; }
    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getStageMovedAt() { return stageMovedAt; }
    public void setStageMovedAt(Instant stageMovedAt) { this.stageMovedAt = stageMovedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public String getClosingNote() { return closingNote; }
    public void setClosingNote(String closingNote) { this.closingNote = closingNote; }
    public UUID getClosedBy() { return closedBy; }
    public void setClosedBy(UUID closedBy) { this.closedBy = closedBy; }
}
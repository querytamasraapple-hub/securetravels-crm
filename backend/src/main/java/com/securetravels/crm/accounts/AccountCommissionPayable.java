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
import java.util.UUID;

/**
 * Phase 7 Module 1 — what SecureTravels owes an account (travel agent)
 * for a confirmed booking. Exactly one row per booking: the unique
 * booking_id is the idempotency guard, mirroring sales_commission_ledger.
 * A credit is never deleted, only VOIDed with a retained reason.
 */
@Entity
@Table(name = "account_commission_payables", indexes = {
        @Index(name = "idx_commission_payables_account_status", columnList = "account_id, status"),
        @Index(name = "idx_commission_payables_status", columnList = "status"),
        @Index(name = "idx_commission_payables_plan", columnList = "plan_id")
})
public class AccountCommissionPayable extends Auditable {

    public enum Status { OPEN, PAID, VOID }

    public enum Basis { NET, GROSS }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    /**
     * Phase 7 Module 4 — the plan whose terms produced this amount. Null for
     * payables written before plans existed (flat default rate). Kept on the
     * row so editing a plan never rewrites what was already owed.
     */
    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "payable_at", nullable = false)
    private Instant payableAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "basis", nullable = false, length = 10)
    private Basis basis = Basis.NET;

    @Column(name = "rate_percent", nullable = false, precision = 6, scale = 2)
    private BigDecimal ratePercent;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "paid_ref", length = 120)
    private String paidRef;

    @Column(name = "settled_by")
    private UUID settledBy;

    @Column(name = "notes")
    private String notes;

    public UUID getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public Instant getPayableAt() { return payableAt; }
    public void setPayableAt(Instant payableAt) { this.payableAt = payableAt; }
    public Basis getBasis() { return basis; }
    public void setBasis(Basis basis) { this.basis = basis; }
    public BigDecimal getRatePercent() { return ratePercent; }
    public void setRatePercent(BigDecimal ratePercent) { this.ratePercent = ratePercent; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public String getPaidRef() { return paidRef; }
    public void setPaidRef(String paidRef) { this.paidRef = paidRef; }
    public UUID getSettledBy() { return settledBy; }
    public void setSettledBy(UUID settledBy) { this.settledBy = settledBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isVoided() { return status == Status.VOID; }

    public void markPaid(UUID paidBy, String ref, Instant at) {
        this.status = Status.PAID;
        this.paidRef = ref;
        this.paidAt = at;
        this.settledBy = paidBy;
    }

    public void voidPayable(String reason) {
        if (status == Status.PAID) {
            throw new IllegalStateException("Cannot void a paid payable for booking " + bookingId);
        }
        this.status = Status.VOID;
        this.notes = reason;
    }
}
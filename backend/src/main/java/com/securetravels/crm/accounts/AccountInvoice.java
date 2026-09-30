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
 * Phase 7 Module 1 — an account-first invoice. Exactly one row per
 * booking (unique booking_id); created when a booking is CONFIRMED and
 * carries an account, billed to the account's billing identity. Retail
 * bookings produce no invoice at all, so Phase 1 retail booking behaviour
 * is unchanged. VOID is the only reversal.
 */
@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoices_account", columnList = "account_id"),
        @Index(name = "idx_invoices_status", columnList = "status")
})
public class AccountInvoice extends Auditable {

    public enum Status { ISSUED, VOID }

    public enum BillingEntity { ACCOUNT, RETAIL }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_ref", nullable = false, unique = true, length = 20)
    private String invoiceRef;

    @Column(name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

    @Column(name = "account_id")
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_entity", nullable = false, length = 20)
    private BillingEntity billingEntity;

    @Column(name = "billing_name", nullable = false, length = 200)
    private String billingName;

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "billing_gstin", length = 15)
    private String billingGstin;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ISSUED;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "issued_by")
    private UUID issuedBy;

    @Column(name = "notes")
    private String notes;

    public UUID getId() { return id; }
    public String getInvoiceRef() { return invoiceRef; }
    public void setInvoiceRef(String invoiceRef) { this.invoiceRef = invoiceRef; }
    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public BillingEntity getBillingEntity() { return billingEntity; }
    public void setBillingEntity(BillingEntity billingEntity) { this.billingEntity = billingEntity; }
    public String getBillingName() { return billingName; }
    public void setBillingName(String billingName) { this.billingName = billingName; }
    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
    public String getBillingGstin() { return billingGstin; }
    public void setBillingGstin(String billingGstin) { this.billingGstin = billingGstin; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public UUID getIssuedBy() { return issuedBy; }
    public void setIssuedBy(UUID issuedBy) { this.issuedBy = issuedBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public void voidInvoice(String reason) {
        if (status == Status.VOID) {
            return;
        }
        this.status = Status.VOID;
        this.notes = reason;
    }
}
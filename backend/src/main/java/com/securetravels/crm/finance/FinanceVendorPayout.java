package com.securetravels.crm.finance;

import com.securetravels.crm.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "finance_vendor_payouts")
public class FinanceVendorPayout extends Auditable {

    public enum Category {
        GUIDE, PORTER, HOTEL, TRANSPORT, PERMITS, FOOD, GEAR, AGENT_COMMISSION, OTHER
    }

    public enum PaymentMethod { CASH, UPI, BANK_TRANSFER, CARD, CHEQUE }

    public enum Status { SCHEDULED, PAID, HOLD, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendor_name", nullable = false, length = 160)
    private String vendorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Category category;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payout_date", nullable = false)
    private LocalDate payoutDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 40)
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.SCHEDULED;

    @Column(name = "reference_no", length = 80)
    private String referenceNo;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    protected FinanceVendorPayout() {}

    public FinanceVendorPayout(String vendorName, Category category, BigDecimal amount, LocalDate payoutDate) {
        this.vendorName = vendorName;
        this.category = category;
        this.amount = amount;
        this.payoutDate = payoutDate;
    }

    public UUID getId() { return id; }
    public String getVendorName() { return vendorName; }
    public Category getCategory() { return category; }
    public UUID getTripId() { return tripId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getPayoutDate() { return payoutDate; }
    public LocalDate getDueDate() { return dueDate; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public Status getStatus() { return status; }
    public String getReferenceNo() { return referenceNo; }
    public String getNotes() { return notes; }
    public UUID getRecordedBy() { return recordedBy; }

    public void setTripId(UUID tripId) { this.tripId = tripId; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setStatus(Status status) { this.status = status; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}

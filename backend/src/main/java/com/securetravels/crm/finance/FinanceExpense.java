package com.securetravels.crm.finance;

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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "finance_expenses", indexes = {
        @Index(name = "idx_finance_expenses_date", columnList = "expense_date"),
        @Index(name = "idx_finance_expenses_category", columnList = "category")
})
public class FinanceExpense extends Auditable {

    public enum Category {
        GUIDE_FEES, TRANSPORT, HOTEL, PERMITS, FOOD, GEAR, MARKETING, SALARIES, INSURANCE, OTHER
    }

    public enum PaymentMethod { CASH, UPI, BANK_TRANSFER, CARD, CHEQUE }

    public enum Status { DRAFT, POSTED, VOID }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Category category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(name = "vendor_name", length = 160)
    private String vendorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 40)
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.POSTED;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    protected FinanceExpense() {}

    public FinanceExpense(Category category, String title, BigDecimal amount, LocalDate expenseDate) {
        this.category = category;
        this.title = title;
        this.amount = amount;
        this.expenseDate = expenseDate;
    }

    public UUID getId() { return id; }
    public Category getCategory() { return category; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public UUID getTripId() { return tripId; }
    public String getVendorName() { return vendorName; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public Status getStatus() { return status; }
    public String getNotes() { return notes; }
    public UUID getRecordedBy() { return recordedBy; }

    public void setCategory(Category category) { this.category = category; }
    public void setTitle(String title) { this.title = title; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public void setTripId(UUID tripId) { this.tripId = tripId; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setStatus(Status status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}

package com.securetravels.crm.basecamp;

import com.securetravels.crm.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "staff_payroll", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "period_month"}))
public class StaffPayroll extends Auditable {

    public enum Status { DRAFT, APPROVED, PAID, VOID }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "period_month", nullable = false, length = 7)
    private String periodMonth;

    @Column(name = "basic_pay", nullable = false, precision = 12, scale = 2)
    private BigDecimal basicPay = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal allowances = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "net_pay", nullable = false, precision = 12, scale = 2)
    private BigDecimal netPay = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    protected StaffPayroll() {}

    public StaffPayroll(UUID userId, String periodMonth) {
        this.userId = userId;
        this.periodMonth = periodMonth;
    }

    public void recalculate() {
        this.netPay = basicPay.add(allowances).subtract(deductions);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getPeriodMonth() { return periodMonth; }
    public BigDecimal getBasicPay() { return basicPay; }
    public BigDecimal getAllowances() { return allowances; }
    public BigDecimal getDeductions() { return deductions; }
    public BigDecimal getNetPay() { return netPay; }
    public Status getStatus() { return status; }
    public String getNotes() { return notes; }
    public UUID getRecordedBy() { return recordedBy; }

    public void setBasicPay(BigDecimal basicPay) { this.basicPay = basicPay; }
    public void setAllowances(BigDecimal allowances) { this.allowances = allowances; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }
    public void setStatus(Status status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}

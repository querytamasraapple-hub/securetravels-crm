package com.securetravels.crm.finance;

import com.securetravels.crm.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "finance_budgets", uniqueConstraints = @UniqueConstraint(columnNames = {"period_month", "category"}))
public class FinanceBudget extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "period_month", nullable = false, length = 7)
    private String periodMonth;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(name = "budget_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal budgetAmount;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    protected FinanceBudget() {}

    public FinanceBudget(String periodMonth, String category, BigDecimal budgetAmount) {
        this.periodMonth = periodMonth;
        this.category = category;
        this.budgetAmount = budgetAmount;
    }

    public UUID getId() { return id; }
    public String getPeriodMonth() { return periodMonth; }
    public String getCategory() { return category; }
    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public String getNotes() { return notes; }
    public UUID getRecordedBy() { return recordedBy; }

    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}

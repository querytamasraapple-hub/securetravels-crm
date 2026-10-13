package com.securetravels.crm.finance;

import com.securetravels.crm.common.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "finance_invoice_lines")
public class FinanceInvoiceLine extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private FinanceInvoice invoice;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(nullable = false, length = 400)
    private String description;

    @Column(name = "hsn_sac", nullable = false, length = 12)
    private String hsnSac = "998555";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(nullable = false, length = 20)
    private String unit = "NOS";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rate;

    @Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstRate = new BigDecimal("5.00");

    protected FinanceInvoiceLine() {}

    public FinanceInvoiceLine(String description, BigDecimal quantity, String unit, BigDecimal rate,
                              BigDecimal taxableAmount, String hsnSac, BigDecimal gstRate) {
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
        this.rate = rate;
        this.taxableAmount = taxableAmount;
        if (hsnSac != null) this.hsnSac = hsnSac;
        if (gstRate != null) this.gstRate = gstRate;
    }

    public UUID getId() { return id; }
    public FinanceInvoice getInvoice() { return invoice; }
    public int getLineNo() { return lineNo; }
    public String getDescription() { return description; }
    public String getHsnSac() { return hsnSac; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public BigDecimal getRate() { return rate; }
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public BigDecimal getGstRate() { return gstRate; }

    public void setInvoice(FinanceInvoice invoice) { this.invoice = invoice; }
    public void setLineNo(int lineNo) { this.lineNo = lineNo; }
}

package com.securetravels.crm.finance;

import com.securetravels.crm.common.audit.Auditable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "finance_invoices")
public class FinanceInvoice extends Auditable {

    public enum Status { DRAFT, SENT, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED }
    public enum InvoiceType { TAX_INVOICE, BILL_OF_SUPPLY, PROFORMA }
    public enum SupplyType { INTRA, INTER }
    public enum WhatsappDeliveryStatus { PENDING, QUEUED, SENT, FAILED, SKIPPED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_no", nullable = false, unique = true, length = 40)
    private String invoiceNo;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "customer_name", nullable = false, length = 160)
    private String customerName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_whatsapp", length = 20)
    private String customerWhatsapp;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "customer_gstin", length = 20)
    private String customerGstin;

    @Column(name = "customer_address", columnDefinition = "text")
    private String customerAddress;

    @Column(name = "customer_state", length = 80)
    private String customerState;

    @Column(name = "customer_state_code", length = 2)
    private String customerStateCode;

    @Column(name = "place_of_supply", length = 80)
    private String placeOfSupply;

    @Column(name = "place_of_supply_code", length = 2)
    private String placeOfSupplyCode;

    @Column(name = "seller_legal_name", length = 200)
    private String sellerLegalName;

    @Column(name = "seller_trade_name", length = 200)
    private String sellerTradeName;

    @Column(name = "seller_gstin", length = 20)
    private String sellerGstin;

    @Column(name = "seller_address", columnDefinition = "text")
    private String sellerAddress;

    @Column(name = "seller_state", length = 80)
    private String sellerState;

    @Column(name = "seller_state_code", length = 2)
    private String sellerStateCode;

    @Column(name = "seller_phone", length = 20)
    private String sellerPhone;

    @Column(name = "seller_email", length = 255)
    private String sellerEmail;

    @Column(name = "seller_bank_name", length = 120)
    private String sellerBankName;

    @Column(name = "seller_bank_account", length = 40)
    private String sellerBankAccount;

    @Column(name = "seller_bank_ifsc", length = 20)
    private String sellerBankIfsc;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 30)
    private InvoiceType invoiceType = InvoiceType.TAX_INVOICE;

    @Enumerated(EnumType.STRING)
    @Column(name = "supply_type", nullable = false, length = 20)
    private SupplyType supplyType = SupplyType.INTRA;

    @Column(name = "reverse_charge", nullable = false)
    private boolean reverseCharge;

    @Column(name = "sac_code", nullable = false, length = 12)
    private String sacCode = "998555";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "cgst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal cgstRate = BigDecimal.ZERO;

    @Column(name = "sgst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal sgstRate = BigDecimal.ZERO;

    @Column(name = "igst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal igstRate = BigDecimal.ZERO;

    @Column(name = "cgst_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(name = "sgst_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(name = "igst_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "amount_in_words", length = 500)
    private String amountInWords;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(columnDefinition = "text")
    private String declaration;

    @Enumerated(EnumType.STRING)
    @Column(name = "whatsapp_delivery_status", nullable = false, length = 20)
    private WhatsappDeliveryStatus whatsappDeliveryStatus = WhatsappDeliveryStatus.PENDING;

    @Column(name = "whatsapp_queued_at")
    private Instant whatsappQueuedAt;

    @Column(name = "whatsapp_sent_at")
    private Instant whatsappSentAt;

    @Column(name = "whatsapp_last_error", columnDefinition = "text")
    private String whatsappLastError;

    @Column(name = "recorded_by")
    private UUID recordedBy;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("lineNo ASC")
    private List<FinanceInvoiceLine> lines = new ArrayList<>();

    protected FinanceInvoice() {}

    public FinanceInvoice(String invoiceNo, String customerName, LocalDate issueDate, LocalDate dueDate,
                          BigDecimal subtotal) {
        this.invoiceNo = invoiceNo;
        this.customerName = customerName;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.subtotal = subtotal;
        this.totalAmount = subtotal;
    }

    public void clearAndAddLines(List<FinanceInvoiceLine> newLines) {
        lines.clear();
        int n = 1;
        for (FinanceInvoiceLine line : newLines) {
            line.setInvoice(this);
            line.setLineNo(n++);
            lines.add(line);
        }
    }

    public UUID getId() { return id; }
    public String getInvoiceNo() { return invoiceNo; }
    public UUID getBookingId() { return bookingId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerWhatsapp() { return customerWhatsapp; }
    public String getCustomerPhone() { return customerPhone; }
    public String getCustomerGstin() { return customerGstin; }
    public String getCustomerAddress() { return customerAddress; }
    public String getCustomerState() { return customerState; }
    public String getCustomerStateCode() { return customerStateCode; }
    public String getPlaceOfSupply() { return placeOfSupply; }
    public String getPlaceOfSupplyCode() { return placeOfSupplyCode; }
    public String getSellerLegalName() { return sellerLegalName; }
    public String getSellerTradeName() { return sellerTradeName; }
    public String getSellerGstin() { return sellerGstin; }
    public String getSellerAddress() { return sellerAddress; }
    public String getSellerState() { return sellerState; }
    public String getSellerStateCode() { return sellerStateCode; }
    public String getSellerPhone() { return sellerPhone; }
    public String getSellerEmail() { return sellerEmail; }
    public String getSellerBankName() { return sellerBankName; }
    public String getSellerBankAccount() { return sellerBankAccount; }
    public String getSellerBankIfsc() { return sellerBankIfsc; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public InvoiceType getInvoiceType() { return invoiceType; }
    public SupplyType getSupplyType() { return supplyType; }
    public boolean isReverseCharge() { return reverseCharge; }
    public String getSacCode() { return sacCode; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getCgstRate() { return cgstRate; }
    public BigDecimal getSgstRate() { return sgstRate; }
    public BigDecimal getIgstRate() { return igstRate; }
    public BigDecimal getCgstAmount() { return cgstAmount; }
    public BigDecimal getSgstAmount() { return sgstAmount; }
    public BigDecimal getIgstAmount() { return igstAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getAmountInWords() { return amountInWords; }
    public String getCurrency() { return currency; }
    public Status getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getDeclaration() { return declaration; }
    public WhatsappDeliveryStatus getWhatsappDeliveryStatus() { return whatsappDeliveryStatus; }
    public Instant getWhatsappQueuedAt() { return whatsappQueuedAt; }
    public Instant getWhatsappSentAt() { return whatsappSentAt; }
    public String getWhatsappLastError() { return whatsappLastError; }
    public UUID getRecordedBy() { return recordedBy; }
    public List<FinanceInvoiceLine> getLines() { return lines; }

    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public void setCustomerWhatsapp(String customerWhatsapp) { this.customerWhatsapp = customerWhatsapp; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public void setCustomerGstin(String customerGstin) { this.customerGstin = customerGstin; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }
    public void setCustomerState(String customerState) { this.customerState = customerState; }
    public void setCustomerStateCode(String customerStateCode) { this.customerStateCode = customerStateCode; }
    public void setPlaceOfSupply(String placeOfSupply) { this.placeOfSupply = placeOfSupply; }
    public void setPlaceOfSupplyCode(String placeOfSupplyCode) { this.placeOfSupplyCode = placeOfSupplyCode; }
    public void setSellerLegalName(String sellerLegalName) { this.sellerLegalName = sellerLegalName; }
    public void setSellerTradeName(String sellerTradeName) { this.sellerTradeName = sellerTradeName; }
    public void setSellerGstin(String sellerGstin) { this.sellerGstin = sellerGstin; }
    public void setSellerAddress(String sellerAddress) { this.sellerAddress = sellerAddress; }
    public void setSellerState(String sellerState) { this.sellerState = sellerState; }
    public void setSellerStateCode(String sellerStateCode) { this.sellerStateCode = sellerStateCode; }
    public void setSellerPhone(String sellerPhone) { this.sellerPhone = sellerPhone; }
    public void setSellerEmail(String sellerEmail) { this.sellerEmail = sellerEmail; }
    public void setSellerBankName(String sellerBankName) { this.sellerBankName = sellerBankName; }
    public void setSellerBankAccount(String sellerBankAccount) { this.sellerBankAccount = sellerBankAccount; }
    public void setSellerBankIfsc(String sellerBankIfsc) { this.sellerBankIfsc = sellerBankIfsc; }
    public void setInvoiceType(InvoiceType invoiceType) { this.invoiceType = invoiceType; }
    public void setSupplyType(SupplyType supplyType) { this.supplyType = supplyType; }
    public void setReverseCharge(boolean reverseCharge) { this.reverseCharge = reverseCharge; }
    public void setSacCode(String sacCode) { this.sacCode = sacCode; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setCgstRate(BigDecimal cgstRate) { this.cgstRate = cgstRate; }
    public void setSgstRate(BigDecimal sgstRate) { this.sgstRate = sgstRate; }
    public void setIgstRate(BigDecimal igstRate) { this.igstRate = igstRate; }
    public void setCgstAmount(BigDecimal cgstAmount) { this.cgstAmount = cgstAmount; }
    public void setSgstAmount(BigDecimal sgstAmount) { this.sgstAmount = sgstAmount; }
    public void setIgstAmount(BigDecimal igstAmount) { this.igstAmount = igstAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setAmountInWords(String amountInWords) { this.amountInWords = amountInWords; }
    public void setStatus(Status status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setDeclaration(String declaration) { this.declaration = declaration; }
    public void setWhatsappDeliveryStatus(WhatsappDeliveryStatus whatsappDeliveryStatus) {
        this.whatsappDeliveryStatus = whatsappDeliveryStatus;
    }
    public void setWhatsappQueuedAt(Instant whatsappQueuedAt) { this.whatsappQueuedAt = whatsappQueuedAt; }
    public void setWhatsappSentAt(Instant whatsappSentAt) { this.whatsappSentAt = whatsappSentAt; }
    public void setWhatsappLastError(String whatsappLastError) { this.whatsappLastError = whatsappLastError; }
    public void setRecordedBy(UUID recordedBy) { this.recordedBy = recordedBy; }
}

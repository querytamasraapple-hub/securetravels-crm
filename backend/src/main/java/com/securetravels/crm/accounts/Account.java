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

import java.util.UUID;

/**
 * Phase 7 Module 1 — an account is a legal entity (corporate) or a travel
 * agent through which leads and bookings are made. Retail customers stay
 * plain {@code customer_360} records; only accounts can carry an invoice
 * in Module 1.
 */
@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_type_active", columnList = "account_type, is_active"),
        @Index(name = "idx_accounts_gstin", columnList = "gstin")
})
public class Account extends Auditable {

    public enum AccountType { CORPORATE, TRAVEL_AGENT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "billing_name", length = 200)
    private String billingName;

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "city", length = 80)
    private String city;

    @Column(name = "primary_contact_name", length = 200)
    private String primaryContactName;

    @Column(name = "primary_contact_email", length = 255)
    private String primaryContactEmail;

    @Column(name = "primary_contact_phone", length = 30)
    private String primaryContactPhone;

    @Column(name = "bank_account_ref", length = 40)
    private String bankAccountRef;

    @Column(name = "credit_terms_days")
    private Integer creditTermsDays;

    @Column(name = "notes")
    private String notes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public UUID getId() { return id; }
    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getBillingName() { return billingName; }
    public void setBillingName(String billingName) { this.billingName = billingName; }
    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String billingAddress) { this.billingAddress = billingAddress; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPrimaryContactName() { return primaryContactName; }
    public void setPrimaryContactName(String primaryContactName) { this.primaryContactName = primaryContactName; }
    public String getPrimaryContactEmail() { return primaryContactEmail; }
    public void setPrimaryContactEmail(String primaryContactEmail) { this.primaryContactEmail = primaryContactEmail; }
    public String getPrimaryContactPhone() { return primaryContactPhone; }
    public void setPrimaryContactPhone(String primaryContactPhone) { this.primaryContactPhone = primaryContactPhone; }
    public String getBankAccountRef() { return bankAccountRef; }
    public void setBankAccountRef(String bankAccountRef) { this.bankAccountRef = bankAccountRef; }
    public Integer getCreditTermsDays() { return creditTermsDays; }
    public void setCreditTermsDays(Integer creditTermsDays) { this.creditTermsDays = creditTermsDays; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
package com.securetravels.crm.accounts;

import com.securetravels.crm.accounts.dto.InvoiceResponse;
import com.securetravels.crm.booking.Booking;
import com.securetravels.crm.common.exception.ForbiddenException;
import com.securetravels.crm.common.exception.NotFoundException;
import com.securetravels.crm.user.UserPrincipal;
import com.securetravels.crm.common.util.XssSanitizer;
import com.securetravels.crm.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Phase 7 Module 1 — account-first invoices. An invoice row is created when
 * a CONFIRMED booking carries an account (billed to the account's billing
 * identity — this is what makes the account "billing-grade" in Module 1).
 * Retail bookings generate no invoice: Phase 1 retail booking behaviour is
 * unchanged, which is the module's no-regression guarantee. VOID is the
 * only reversal. Full invoicing (period-level line items, payments against
 * invoices, e-invoicing) stays Phase 9.
 */
@Service
public class AccountInvoiceService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(AccountInvoiceService.class);

    private static final Set<Role> MANAGER_AND_UP = EnumSet.of(Role.MANAGER, Role.ADMIN, Role.CEO);

    private final AccountInvoiceRepository invoices;
    private final AccountRepository accounts;

    public AccountInvoiceService(AccountInvoiceRepository invoices, AccountRepository accounts) {
        this.invoices = invoices;
        this.accounts = accounts;
    }

    @Transactional
    public AccountInvoice issueForBooking(Booking booking, UUID issuedBy) {
        if (booking.getAccountId() == null) {
            return null;   // retail — no invoice, ever
        }
        AccountInvoice existing = invoices.findByBookingId(booking.getId()).orElse(null);
        if (existing != null) {
            return existing;   // idempotent confirm
        }
        Account account = accounts.findById(booking.getAccountId()).orElse(null);
        if (account == null) {
            log.warn("[account-invoice] booking {} references missing account {}; no invoice issued",
                    booking.getId(), booking.getAccountId());
            return null;
        }

        BigDecimal gross = nz(booking.getTotalAmount());
        BigDecimal discount = nz(booking.getDiscountAmount());
        BigDecimal tax = nz(booking.getTaxAmount());

        Instant now = Instant.now();
        AccountInvoice invoice = new AccountInvoice();
        invoice.setBookingId(booking.getId());
        invoice.setAccountId(account.getId());
        invoice.setBillingEntity(AccountInvoice.BillingEntity.ACCOUNT);
        invoice.setBillingName(XssSanitizer.text(
                account.getBillingName() != null ? account.getBillingName() : account.getName()));
        invoice.setBillingAddress(XssSanitizer.text(account.getBillingAddress()));
        invoice.setBillingGstin(account.getGstin());
        invoice.setGrossAmount(gross);
        invoice.setDiscountAmount(discount);
        invoice.setTaxAmount(tax);
        invoice.setNetAmount(gross.subtract(discount).add(tax));
        invoice.setStatus(AccountInvoice.Status.ISSUED);
        invoice.setIssuedAt(now);
        invoice.setIssuedBy(issuedBy);
        invoice.setInvoiceRef(nextRef(now));

        AccountInvoice saved = invoices.save(invoice);
        log.info("[account-invoice] issued {} for booking {}", saved.getInvoiceRef(), booking.getId());
        return saved;
    }

    @Transactional
    public void voidForBooking(UUID bookingId, String reason) {
        invoices.findByBookingId(bookingId).ifPresent(invoice -> {
            if (invoice.getStatus() != AccountInvoice.Status.VOID) {
                invoice.voidInvoice(reason);
                invoices.save(invoice);
                log.info("[account-invoice] voided {} for booking {} ({})",
                        invoice.getInvoiceRef(), bookingId, reason);
            }
        });
    }

    /** Manager-and-up: invoices are customer-facing money documents. */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> listForAccount(UUID accountId, UserPrincipal caller) {
        requireManager(caller);
        return invoices.findByAccountIdOrderByIssuedAtDesc(accountId).stream()
                .map(AccountInvoiceService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(UUID id, UserPrincipal caller) {
        requireManager(caller);
        return toResponse(invoices.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id)));
    }

    private static void requireManager(UserPrincipal caller) {
        if (!MANAGER_AND_UP.contains(caller.role())) {
            throw new ForbiddenException("Invoices are restricted to managers");
        }
    }

    /** INV-YYYY-#### — zero-padded, so string ordering equals numeric ordering. */
    private String nextRef(Instant at) {
        int year = LocalDate.ofInstant(at, ZoneId.systemDefault()).getYear();
        String prefix = "INV-" + year + "-";
        int seq = invoices.findFirstByInvoiceRefStartingWithOrderByInvoiceRefDesc(prefix)
                .map(AccountInvoice::getInvoiceRef)
                .map(ref -> Integer.parseInt(ref.substring(ref.lastIndexOf('-') + 1)) + 1)
                .orElse(1);
        return prefix + String.format("%04d", seq);
    }

    public static InvoiceResponse toResponse(AccountInvoice i) {
        return new InvoiceResponse(
                i.getId(), i.getInvoiceRef(), i.getBookingId(), i.getAccountId(), i.getBillingEntity(),
                i.getBillingName(), i.getBillingAddress(), i.getBillingGstin(), i.getGrossAmount(),
                i.getDiscountAmount(), i.getTaxAmount(), i.getNetAmount(), i.getStatus(),
                i.getIssuedAt(), i.getNotes());
    }

    private static BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}

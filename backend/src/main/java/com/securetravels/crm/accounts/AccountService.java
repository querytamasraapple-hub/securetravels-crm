package com.securetravels.crm.accounts;

import com.securetravels.crm.accounts.dto.AccountCreateRequest;
import com.securetravels.crm.accounts.dto.AccountDetailResponse;
import com.securetravels.crm.accounts.dto.AccountResponse;
import com.securetravels.crm.accounts.dto.AccountUpdateRequest;
import com.securetravels.crm.booking.Booking;
import com.securetravels.crm.booking.BookingRepository;
import com.securetravels.crm.common.audit.AuditAction;
import com.securetravels.crm.common.audit.AuditService;
import com.securetravels.crm.common.exception.ConflictException;
import com.securetravels.crm.common.exception.ForbiddenException;
import com.securetravels.crm.common.exception.NotFoundException;
import com.securetravels.crm.common.util.GstinValidator;
import com.securetravels.crm.common.util.XssSanitizer;
import com.securetravels.crm.lead.LeadRepository;
import com.securetravels.crm.user.Role;
import com.securetravels.crm.user.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Phase 7 Module 1 — account records and their audit-of-record view. Write
 * access is manager-and-up; any authenticated user may read non-financial
 * account data. The 360 detail exposes revenue and payable aggregates, so
 * it is manager-only.
 *
 * <p>GSTIN is validated (structure + mod-36 checksum) and normalized here,
 * and is unique across active accounts so a billing identity cannot be
 * reused by two companies.
 */
@Service
public class AccountService {

    private static final Set<Role> MANAGER_AND_UP = EnumSet.of(Role.MANAGER, Role.ADMIN, Role.CEO);
    private static final List<Booking.Status> RECOGNIZED_BOOKINGS =
            List.of(Booking.Status.CONFIRMED, Booking.Status.COMPLETED);

    private final AccountRepository accounts;
    private final AccountCommissionPayableRepository payables;
    private final LeadRepository leads;
    private final BookingRepository bookings;
    private final AuditService auditService;

    public AccountService(AccountRepository accounts, AccountCommissionPayableRepository payables,
                          LeadRepository leads, BookingRepository bookings, AuditService auditService) {
        this.accounts = accounts;
        this.payables = payables;
        this.leads = leads;
        this.bookings = bookings;
        this.auditService = auditService;
    }

    @Transactional
    public AccountResponse create(AccountCreateRequest request, UserPrincipal caller) {
        requireManager(caller.role());

        Account account = new Account();
        account.setAccountType(request.accountType());
        account.setName(XssSanitizer.text(request.name()));
        account.setGstin(normalizeGstin(request.gstin(), null));
        account.setBillingName(XssSanitizer.text(request.billingName()));
        account.setBillingAddress(XssSanitizer.text(request.billingAddress()));
        account.setCity(XssSanitizer.text(request.city()));
        account.setPrimaryContactName(XssSanitizer.text(request.primaryContactName()));
        account.setPrimaryContactEmail(cleanEmail(request.primaryContactEmail()));
        account.setPrimaryContactPhone(XssSanitizer.text(request.primaryContactPhone()));
        account.setBankAccountRef(XssSanitizer.text(request.bankAccountRef()));
        account.setCreditTermsDays(request.creditTermsDays());
        account.setNotes(XssSanitizer.text(request.notes()));
        account.setActive(true);

        Account saved = accounts.save(account);
        auditService.record("ACCOUNT", saved.getId(), AuditAction.CREATE, "name", null, saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public AccountResponse update(UUID id, AccountUpdateRequest request, UserPrincipal caller) {
        requireManager(caller.role());
        Account account = requireAccount(id);

        if (request.accountType() != null) account.setAccountType(request.accountType());
        if (request.name() != null) account.setName(XssSanitizer.text(request.name()));
        if (request.gstin() != null) account.setGstin(normalizeGstin(request.gstin(), account.getId()));
        if (request.billingName() != null) account.setBillingName(XssSanitizer.text(request.billingName()));
        if (request.billingAddress() != null) account.setBillingAddress(XssSanitizer.text(request.billingAddress()));
        if (request.city() != null) account.setCity(XssSanitizer.text(request.city()));
        if (request.primaryContactName() != null) account.setPrimaryContactName(XssSanitizer.text(request.primaryContactName()));
        if (request.primaryContactEmail() != null) account.setPrimaryContactEmail(cleanEmail(request.primaryContactEmail()));
        if (request.primaryContactPhone() != null) account.setPrimaryContactPhone(XssSanitizer.text(request.primaryContactPhone()));
        if (request.bankAccountRef() != null) account.setBankAccountRef(XssSanitizer.text(request.bankAccountRef()));
        if (request.creditTermsDays() != null) account.setCreditTermsDays(request.creditTermsDays());
        if (request.notes() != null) account.setNotes(XssSanitizer.text(request.notes()));
        if (request.active() != null) account.setActive(request.active());

        auditService.record("ACCOUNT", account.getId(), AuditAction.UPDATE, "name", null, account.getName());
        return toResponse(accounts.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list(Account.AccountType type, boolean activeOnly, UserPrincipal caller) {
        List<Account> rows = type == null
                ? accounts.findByActiveOrderByNameAsc(activeOnly)
                : accounts.findByAccountTypeAndActiveOrderByNameAsc(type, activeOnly);
        return rows.stream().map(AccountService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID id, UserPrincipal caller) {
        return toResponse(requireAccount(id));
    }

    @Transactional(readOnly = true)
    public AccountDetailResponse detail(UUID id, UserPrincipal caller) {
        requireManager(caller.role());
        Account account = requireAccount(id);

        AccountDetailResponse.Stats stats = new AccountDetailResponse.Stats(
                leads.countByAccountId(id),
                bookings.countByAccountId(id),
                bookings.countByAccountIdAndStatusIn(id, RECOGNIZED_BOOKINGS),
                bookings.revenueForAccount(id, RECOGNIZED_BOOKINGS),
                payables.sumOpenByAccount(id),
                payables.sumPaidByAccount(id));

        List<AccountDetailResponse.PayableSummary> payableRows =
                payables.findByAccountIdOrderByPayableAtDesc(id).stream().map(p -> new AccountDetailResponse.PayableSummary(
                        p.getId(), p.getBookingId(),
                        bookings.findById(p.getBookingId()).map(Booking::getBookingRef).orElse(null),
                        p.getStatus().name(), p.getCommissionAmount(), p.getPayableAt()))
                .toList();

        return toDetail(account, stats, bookings.findTop5ByAccountIdOrderByCreatedAtDesc(id), payableRows);
    }

    /**
     * Cross-module resolution: only an existing, active account can accept
     * new leads/booking links. Returns null for a missing or inactive id so
     * callers can decide their own error semantics.
     */
    @Transactional(readOnly = true)
    public Account findActiveAccount(UUID id) {
        if (id == null) return null;
        return accounts.findByIdAndActiveTrue(id).orElse(null);
    }

    // ------------------------------------------------------------------ helpers

    private String normalizeGstin(String raw, UUID currentId) {
        String gstin = GstinValidator.normalizeOrNull(raw);
        if (gstin != null) {
            accounts.findByGstin(gstin).ifPresent(existing -> {
                if (!existing.getId().equals(currentId)) {
                    throw new ConflictException("An account with this GSTIN already exists: " + gstin);
                }
            });
        }
        return gstin;
    }

    private Account requireAccount(UUID id) {
        return accounts.findById(id).orElseThrow(() -> new NotFoundException("Account not found: " + id));
    }

    private static String cleanEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void requireManager(Role actual) {
        if (!MANAGER_AND_UP.contains(actual)) {
            throw new ForbiddenException("Only managers can manage accounts");
        }
    }

    private static BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private static AccountResponse toResponse(Account a) {
        return new AccountResponse(a.getId(), a.getAccountType(), a.getName(), a.getGstin(),
                a.getBillingName(), a.getCity(), a.isActive(), a.getCreatedAt(), a.getUpdatedAt());
    }

    private static AccountDetailResponse toDetail(Account a, AccountDetailResponse.Stats stats,
                                                  List<Booking> recent,
                                                  List<AccountDetailResponse.PayableSummary> payableRows) {
        List<AccountDetailResponse.BookingSummary> bookings = recent.stream()
                .map(b -> new AccountDetailResponse.BookingSummary(b.getId(), b.getBookingRef(),
                        b.getStatus().name(), b.getTravelDate(),
                        nz(b.getTotalAmount()).subtract(nz(b.getDiscountAmount())).add(nz(b.getTaxAmount()))))
                .toList();
        return new AccountDetailResponse(
                a.getId(), a.getAccountType(), a.getName(), a.getGstin(), a.getBillingName(),
                a.getBillingAddress(), a.getCity(), a.getPrimaryContactName(), a.getPrimaryContactEmail(),
                a.getPrimaryContactPhone(), a.getBankAccountRef(), a.getCreditTermsDays(), a.getNotes(),
                a.isActive(), a.getCreatedAt(), a.getUpdatedAt(), stats, bookings, payableRows);
    }
}
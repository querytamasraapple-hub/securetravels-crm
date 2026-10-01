package com.securetravels.crm.accounts;

import com.securetravels.crm.accounts.dto.CommissionPayableResponse;
import com.securetravels.crm.booking.Booking;
import com.securetravels.crm.commission.CommissionPlanService;
import com.securetravels.crm.commission.CommissionQuote;
import com.securetravels.crm.common.exception.BadRequestException;
import com.securetravels.crm.common.exception.ForbiddenException;
import com.securetravels.crm.common.exception.NotFoundException;
import com.securetravels.crm.user.UserPrincipal;
import com.securetravels.crm.common.util.XssSanitizer;
import com.securetravels.crm.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Phase 7 Module 1 — travel-agent commission payable. A payable is created
 * exactly once per CONFIRMED account booking (the unique booking_id is the
 * idempotency guard, mirroring sales_commission_ledger) and is VOIDed —
 * never deleted — when that booking is cancelled.
 *
 * <p>Phase 7 Module 4 changed <em>how</em> the amount is derived: the account's
 * assigned commission plan decides it, falling back to the flat
 * {@code app.commission.default-travel-agent-percent} rate when the account has
 * no plan. Everything else about this service — one row per booking,
 * VOID-not-delete, settlement trail — is unchanged.
 */
@Service
public class AccountCommissionPayableService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(AccountCommissionPayableService.class);

    private static final Set<Role> MANAGER_AND_UP = EnumSet.of(Role.MANAGER, Role.ADMIN, Role.CEO);

    private final AccountCommissionPayableRepository payables;
    private final CommissionPlanService commissionPlans;

    public AccountCommissionPayableService(AccountCommissionPayableRepository payables,
                                           CommissionPlanService commissionPlans) {
        this.payables = payables;
        this.commissionPlans = commissionPlans;
    }

    /**
     * Accrue the commission this booking's account is owed under its current
     * terms. Returns empty when the booking falls below the plan's minimum
     * sales threshold — no payable row is written, because a row that can never
     * be settled is noise in the payable ledger, and the skip is logged.
     */
    @Transactional
    public Optional<AccountCommissionPayable> credit(Booking booking, Account account, Instant at) {
        if (payables.existsByBookingId(booking.getId())) {
            return payables.findByBookingId(booking.getId());
        }
        CommissionQuote quote = commissionPlans.quoteForBooking(booking);
        if (quote.belowThreshold()) {
            log.info("[account-commission] no payable for booking {} on account {}: basis {} is below the {} threshold",
                    booking.getId(), account.getId(), quote.basisAmount(), quote.minSalesThreshold());
            return Optional.empty();
        }

        BigDecimal gross = nz(booking.getTotalAmount());
        BigDecimal discount = nz(booking.getDiscountAmount());
        BigDecimal tax = nz(booking.getTaxAmount());
        BigDecimal net = gross.subtract(discount).add(tax);
        AccountCommissionPayable.Basis basis = AccountCommissionPayable.Basis.valueOf(quote.basis().name());

        AccountCommissionPayable payable = new AccountCommissionPayable();
        payable.setBookingId(booking.getId());
        payable.setAccountId(account.getId());
        payable.setPayableAt(at);
        payable.setBasis(basis);
        // For a FIXED plan there is no percentage, so the rate column records 0
        // rather than a fabricated one; the plan id says which terms applied.
        payable.setRatePercent(quote.appliedRatePercent() == null
                ? BigDecimal.ZERO.setScale(2)
                : quote.appliedRatePercent());
        payable.setPlanId(quote.planId());
        payable.setGrossAmount(gross);
        payable.setDiscountAmount(discount);
        payable.setTaxAmount(tax);
        payable.setNetAmount(basis == AccountCommissionPayable.Basis.GROSS ? gross : net);
        payable.setCommissionAmount(quote.commissionAmount());

        AccountCommissionPayable saved = payables.save(payable);
        log.info("[account-commission] credited {} on booking {} for account {} via plan {} ({})",
                saved.getCommissionAmount(), booking.getId(), account.getId(),
                quote.planKey(), quote.method());
        return Optional.of(saved);
    }

    @Transactional
    public void voidForBooking(UUID bookingId, String reason) {
        payables.findByBookingId(bookingId).ifPresent(payable -> {
            if (payable.getStatus() == AccountCommissionPayable.Status.OPEN) {
                payable.voidPayable(reason);
                payables.save(payable);
            } else if (payable.getStatus() == AccountCommissionPayable.Status.PAID) {
                // Money has already moved; a real clawback needs a human. Failing
                // the booking cancellation for it would be worse, so record the
                // discrepancy and let ops chase it outside the transactional path.
                log.warn("[account-commission] booking {} was cancelled but its payable {} is PAID — "
                        + "manual clawback required on account {}", bookingId, payable.getId(), payable.getAccountId());
            }
        });
    }

    /** A manager settles an OPEN payable against a real outward transfer. */
    @Transactional
    public void markPaid(UUID payableId, UserPrincipal caller, String ref, Instant at) {
        requireManager(caller);
        AccountCommissionPayable payable = payables.findById(payableId)
                .orElseThrow(() -> new NotFoundException("Payable not found: " + payableId));
        if (payable.getStatus() == AccountCommissionPayable.Status.VOID) {
            throw new BadRequestException("Cannot settle a VOID payable");
        }
        payable.markPaid(caller.id(), XssSanitizer.text(ref), at);
        payables.save(payable);
        log.info("[account-commission] settled {} for booking {} ({} by {})",
                payable.getCommissionAmount(), payable.getBookingId(), ref, caller.id());
    }

    /**
     * Manager-and-up: payables are money owed to a partner, so this is not part
     * of the sales-visible account surface (SECURITY.md, Module 1 row).
     */
    @Transactional(readOnly = true)
    public List<CommissionPayableResponse> listForAccount(UUID accountId, UserPrincipal caller) {
        requireManager(caller);
        return payables.findByAccountIdOrderByPayableAtDesc(accountId).stream()
                .map(AccountCommissionPayableService::toResponse)
                .toList();
    }

    private static void requireManager(UserPrincipal caller) {
        if (!MANAGER_AND_UP.contains(caller.role())) {
            throw new ForbiddenException("Settlement data is restricted to managers");
        }
    }

    public static CommissionPayableResponse toResponse(AccountCommissionPayable p) {
        return new CommissionPayableResponse(
                p.getId(), p.getBookingId(), p.getAccountId(), p.getPlanId(), p.getPayableAt(), p.getBasis(),
                p.getRatePercent(), p.getGrossAmount(), p.getDiscountAmount(), p.getTaxAmount(),
                p.getNetAmount(), p.getCommissionAmount(), p.getStatus(), p.getPaidAt(),
                p.getPaidRef(), p.getNotes());
    }

    private static BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}

package com.securetravels.crm.accounts;

import com.securetravels.crm.accounts.dto.CommissionPayableResponse;
import com.securetravels.crm.booking.Booking;
import com.securetravels.crm.common.config.AppProperties;
import com.securetravels.crm.common.exception.BadRequestException;
import com.securetravels.crm.common.exception.NotFoundException;
import com.securetravels.crm.common.util.XssSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Phase 7 Module 1 — travel-agent commission payable. A payable is created
 * exactly once per CONFIRMED account booking (the unique booking_id is the
 * idempotency guard, mirroring sales_commission_ledger) and is VOIDed —
 * never deleted — when that booking is cancelled. Module 4 replaces the
 * flat {@link AppProperties.Commission#getDefaultTravelAgentPercent()} rate
 * with per-plan tiers; the rest of this service's contract is unchanged.
 */
@Service
public class AccountCommissionPayableService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(AccountCommissionPayableService.class);

    private final AccountCommissionPayableRepository payables;
    private final AppProperties appProperties;

    public AccountCommissionPayableService(AccountCommissionPayableRepository payables,
                                           AppProperties appProperties) {
        this.payables = payables;
        this.appProperties = appProperties;
    }

    /** Net = gross - discount + tax, the same definition as V13's ledger. */
    @Transactional
    public AccountCommissionPayable credit(Booking booking, Account account, Instant at) {
        if (payables.existsByBookingId(booking.getId())) {
            return payables.findByBookingId(booking.getId())
                    .orElseThrow(() -> new IllegalStateException("Payable vanished after existence check"));
        }
        BigDecimal gross = nz(booking.getTotalAmount());
        BigDecimal discount = nz(booking.getDiscountAmount());
        BigDecimal tax = nz(booking.getTaxAmount());
        BigDecimal net = gross.subtract(discount).add(tax);
        BigDecimal rate = appProperties.getCommission().getDefaultTravelAgentPercent();

        AccountCommissionPayable payable = new AccountCommissionPayable();
        payable.setBookingId(booking.getId());
        payable.setAccountId(account.getId());
        payable.setPayableAt(at);
        payable.setBasis(AccountCommissionPayable.Basis.NET);
        payable.setRatePercent(rate);
        payable.setGrossAmount(gross);
        payable.setDiscountAmount(discount);
        payable.setTaxAmount(tax);
        payable.setNetAmount(net);
        payable.setCommissionAmount(net.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));

        AccountCommissionPayable saved = payables.save(payable);
        log.info("[account-commission] credited {} {}% on booking {} for account {}",
                saved.getCommissionAmount(), rate, booking.getId(), account.getId());
        return saved;
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
    public void markPaid(UUID payableId, UUID settledBy, String ref, Instant at) {
        AccountCommissionPayable payable = payables.findById(payableId)
                .orElseThrow(() -> new NotFoundException("Payable not found: " + payableId));
        if (payable.getStatus() == AccountCommissionPayable.Status.VOID) {
            throw new BadRequestException("Cannot settle a VOID payable");
        }
        payable.markPaid(settledBy, XssSanitizer.text(ref), at);
        payables.save(payable);
        log.info("[account-commission] settled {} for booking {} ({} by {})",
                payable.getCommissionAmount(), payable.getBookingId(), ref, settledBy);
    }

    @Transactional(readOnly = true)
    public List<CommissionPayableResponse> listForAccount(UUID accountId) {
        return payables.findByAccountIdOrderByPayableAtDesc(accountId).stream()
                .map(AccountCommissionPayableService::toResponse)
                .toList();
    }

    public static CommissionPayableResponse toResponse(AccountCommissionPayable p) {
        return new CommissionPayableResponse(
                p.getId(), p.getBookingId(), p.getAccountId(), p.getPayableAt(), p.getBasis(),
                p.getRatePercent(), p.getGrossAmount(), p.getDiscountAmount(), p.getTaxAmount(),
                p.getNetAmount(), p.getCommissionAmount(), p.getStatus(), p.getPaidAt(),
                p.getPaidRef(), p.getNotes());
    }

    private static BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
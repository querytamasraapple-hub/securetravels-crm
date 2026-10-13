package com.securetravels.crm.finance;

import com.securetravels.crm.booking.Booking;
import com.securetravels.crm.booking.BookingRepository;
import com.securetravels.crm.common.exception.ForbiddenException;
import com.securetravels.crm.common.exception.NotFoundException;
import com.securetravels.crm.common.util.XssSanitizer;
import com.securetravels.crm.finance.dto.FinanceExpenseRequest;
import com.securetravels.crm.finance.dto.FinanceExpenseResponse;
import com.securetravels.crm.finance.dto.FinanceOverviewResponse;
import com.securetravels.crm.finance.dto.FinancePnLResponse;
import com.securetravels.crm.payment.Payment;
import com.securetravels.crm.payment.PaymentRepository;
import com.securetravels.crm.trip.Trip;
import com.securetravels.crm.trip.TripRepository;
import com.securetravels.crm.user.Role;
import com.securetravels.crm.user.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    private static final Set<Role> FINANCE_ACCESS = EnumSet.of(
            Role.FINANCE, Role.MANAGER, Role.ADMIN, Role.CEO);

    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final FinanceExpenseRepository expenses;
    private final TripRepository trips;

    public FinanceService(PaymentRepository payments, BookingRepository bookings,
                          FinanceExpenseRepository expenses, TripRepository trips) {
        this.payments = payments;
        this.bookings = bookings;
        this.expenses = expenses;
        this.trips = trips;
    }

    @Transactional(readOnly = true)
    public FinanceOverviewResponse overview(UserPrincipal caller, LocalDate from, LocalDate to) {
        requireFinance(caller);
        LocalDate rangeFrom = from != null ? from : LocalDate.now().minusMonths(5).withDayOfMonth(1);
        LocalDate rangeTo = to != null ? to : LocalDate.now();

        List<Payment> allPayments = payments.findAll();
        List<Booking> allBookings = bookings.findAll();
        List<FinanceExpense> allExpenses = expenses.findByExpenseDateBetweenOrderByExpenseDateDesc(rangeFrom, rangeTo);
        Map<UUID, String> tripNames = trips.findAll().stream()
                .collect(Collectors.toMap(Trip::getId, Trip::getName, (a, b) -> a));
        Map<UUID, Booking> bookingById = allBookings.stream()
                .collect(Collectors.toMap(Booking::getId, b -> b, (a, b) -> a));

        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        BigDecimal overdue = BigDecimal.ZERO;
        BigDecimal refunded = BigDecimal.ZERO;
        long completedCount = 0;
        long overdueCount = 0;
        Map<Payment.Status, BigDecimal> byStatusAmt = new EnumMap<>(Payment.Status.class);
        Map<Payment.Status, Long> byStatusCnt = new EnumMap<>(Payment.Status.class);
        Map<String, BigDecimal> monthCollected = new LinkedHashMap<>();
        Map<String, BigDecimal> monthOutstanding = new LinkedHashMap<>();
        Map<String, BigDecimal> revenueByTrip = new LinkedHashMap<>();

        for (YearMonth ym = YearMonth.from(rangeFrom); !ym.isAfter(YearMonth.from(rangeTo)); ym = ym.plusMonths(1)) {
            String key = ym.toString();
            monthCollected.put(key, BigDecimal.ZERO);
            monthOutstanding.put(key, BigDecimal.ZERO);
        }

        for (Payment p : allPayments) {
            byStatusAmt.merge(p.getStatus(), p.getAmount(), BigDecimal::add);
            byStatusCnt.merge(p.getStatus(), 1L, Long::sum);

            switch (p.getStatus()) {
                case COMPLETED, PARTIAL -> {
                    collected = collected.add(p.getAmount());
                    completedCount++;
                    String monthKey = monthKey(p);
                    if (monthCollected.containsKey(monthKey)) {
                        monthCollected.merge(monthKey, p.getAmount(), BigDecimal::add);
                    }
                    Booking b = bookingById.get(p.getBookingId());
                    if (b != null) {
                        String trip = tripNames.getOrDefault(b.getTripId(), "Unknown trek");
                        revenueByTrip.merge(trip, p.getAmount(), BigDecimal::add);
                    }
                }
                case PENDING, OVERDUE -> {
                    outstanding = outstanding.add(p.getAmount());
                    if (p.getStatus() == Payment.Status.OVERDUE
                            || (p.getDueDate() != null && p.getDueDate().isBefore(LocalDate.now()))) {
                        overdue = overdue.add(p.getAmount());
                        overdueCount++;
                    }
                    String monthKey = p.getDueDate() != null
                            ? YearMonth.from(p.getDueDate()).toString()
                            : monthKey(p);
                    if (monthOutstanding.containsKey(monthKey)) {
                        monthOutstanding.merge(monthKey, p.getAmount(), BigDecimal::add);
                    }
                }
                case REFUNDED -> refunded = refunded.add(p.getAmount());
                case CANCELLED -> { /* ignore */ }
            }
        }

        BigDecimal pipeline = allBookings.stream()
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED || b.getStatus() == Booking.Status.QUOTATION)
                .map(Booking::netAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long confirmedBookings = allBookings.stream()
                .filter(b -> b.getStatus() == Booking.Status.CONFIRMED || b.getStatus() == Booking.Status.COMPLETED)
                .count();

        BigDecimal expenseTotal = allExpenses.stream()
                .filter(e -> e.getStatus() == FinanceExpense.Status.POSTED)
                .map(FinanceExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> monthExpenses = new LinkedHashMap<>();
        for (String key : monthCollected.keySet()) monthExpenses.put(key, BigDecimal.ZERO);
        Map<String, BigDecimal> expensesByCat = new LinkedHashMap<>();
        Map<String, Long> expensesByCatCnt = new LinkedHashMap<>();
        for (FinanceExpense e : allExpenses) {
            if (e.getStatus() != FinanceExpense.Status.POSTED) continue;
            String mk = YearMonth.from(e.getExpenseDate()).toString();
            if (monthExpenses.containsKey(mk)) {
                monthExpenses.merge(mk, e.getAmount(), BigDecimal::add);
            }
            String cat = e.getCategory().name();
            expensesByCat.merge(cat, e.getAmount(), BigDecimal::add);
            expensesByCatCnt.merge(cat, 1L, Long::sum);
        }

        BigDecimal net = collected.subtract(expenseTotal);
        BigDecimal denom = collected.add(outstanding);
        BigDecimal collectionRate = denom.signum() == 0
                ? BigDecimal.ZERO
                : collected.multiply(BigDecimal.valueOf(100)).divide(denom, 1, RoundingMode.HALF_UP);

        List<FinanceOverviewResponse.MonthPoint> series = new ArrayList<>();
        for (String key : monthCollected.keySet()) {
            series.add(new FinanceOverviewResponse.MonthPoint(
                    key, monthCollected.get(key), monthExpenses.getOrDefault(key, BigDecimal.ZERO),
                    monthOutstanding.getOrDefault(key, BigDecimal.ZERO)));
        }

        List<FinanceOverviewResponse.NamedAmount> tripRev = revenueByTrip.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(8)
                .map(e -> new FinanceOverviewResponse.NamedAmount(e.getKey(), e.getValue(), 0))
                .toList();

        List<FinanceOverviewResponse.NamedAmount> catExp = expensesByCat.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new FinanceOverviewResponse.NamedAmount(
                        e.getKey(), e.getValue(), expensesByCatCnt.getOrDefault(e.getKey(), 0L)))
                .toList();

        List<FinanceOverviewResponse.StatusBucket> statusBuckets = byStatusAmt.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .map(e -> new FinanceOverviewResponse.StatusBucket(
                        e.getKey().name(), e.getValue(), byStatusCnt.getOrDefault(e.getKey(), 0L)))
                .toList();

        return new FinanceOverviewResponse(
                collected, outstanding, overdue, refunded, pipeline, expenseTotal, net, collectionRate,
                completedCount, overdueCount, confirmedBookings, series, tripRev, catExp, statusBuckets);
    }

    @Transactional(readOnly = true)
    public FinancePnLResponse pnl(UserPrincipal caller, LocalDate from, LocalDate to) {
        requireFinance(caller);
        LocalDate rangeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate rangeTo = to != null ? to : LocalDate.now();
        FinanceOverviewResponse ov = overview(caller, rangeFrom, rangeTo);

        BigDecimal revenue = ov.monthlySeries().stream()
                .map(FinanceOverviewResponse.MonthPoint::collected)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Prefer full collected filtered by paid period when possible
        revenue = payments.findAll().stream()
                .filter(p -> p.getStatus() == Payment.Status.COMPLETED || p.getStatus() == Payment.Status.PARTIAL)
                .filter(p -> {
                    LocalDate d = p.getPaidAt() != null
                            ? p.getPaidAt().atZone(ZoneOffset.UTC).toLocalDate()
                            : (p.getCreatedAt() != null
                            ? p.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate()
                            : null);
                    return d != null && !d.isBefore(rangeFrom) && !d.isAfter(rangeTo);
                })
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenseTotal = ov.totalExpenses();
        BigDecimal profit = revenue.subtract(expenseTotal);
        BigDecimal margin = revenue.signum() == 0
                ? BigDecimal.ZERO
                : profit.multiply(BigDecimal.valueOf(100)).divide(revenue, 1, RoundingMode.HALF_UP);

        return new FinancePnLResponse(
                rangeFrom.toString(), rangeTo.toString(), revenue, expenseTotal, profit, margin,
                ov.expensesByCategory(), ov.revenueByTrip());
    }

    @Transactional(readOnly = true)
    public List<FinanceExpenseResponse> listExpenses(UserPrincipal caller, LocalDate from, LocalDate to) {
        requireFinance(caller);
        List<FinanceExpense> rows = (from != null && to != null)
                ? expenses.findByExpenseDateBetweenOrderByExpenseDateDesc(from, to)
                : expenses.findAllByOrderByExpenseDateDesc();
        Map<UUID, String> tripNames = trips.findAll().stream()
                .collect(Collectors.toMap(Trip::getId, Trip::getName, (a, b) -> a));
        return rows.stream()
                .map(e -> FinanceExpenseResponse.from(e, e.getTripId() == null ? null : tripNames.get(e.getTripId())))
                .toList();
    }

    @Transactional
    public FinanceExpenseResponse createExpense(FinanceExpenseRequest request, UserPrincipal caller) {
        requireFinanceWrite(caller);
        FinanceExpense e = new FinanceExpense(
                request.category(),
                XssSanitizer.text(request.title()),
                request.amount(),
                request.expenseDate());
        e.setTripId(request.tripId());
        e.setVendorName(XssSanitizer.text(request.vendorName()));
        e.setPaymentMethod(request.paymentMethod() != null ? request.paymentMethod() : FinanceExpense.PaymentMethod.BANK_TRANSFER);
        e.setStatus(request.status() != null ? request.status() : FinanceExpense.Status.POSTED);
        e.setNotes(XssSanitizer.text(request.notes()));
        e.setRecordedBy(caller.id());
        FinanceExpense saved = expenses.save(e);
        String tripName = saved.getTripId() == null ? null
                : trips.findById(saved.getTripId()).map(Trip::getName).orElse(null);
        return FinanceExpenseResponse.from(saved, tripName);
    }

    @Transactional
    public FinanceExpenseResponse voidExpense(UUID id, UserPrincipal caller) {
        requireFinanceWrite(caller);
        FinanceExpense e = expenses.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense not found: " + id));
        e.setStatus(FinanceExpense.Status.VOID);
        FinanceExpense saved = expenses.save(e);
        String tripName = saved.getTripId() == null ? null
                : trips.findById(saved.getTripId()).map(Trip::getName).orElse(null);
        return FinanceExpenseResponse.from(saved, tripName);
    }

    private static String monthKey(Payment p) {
        if (p.getPaidAt() != null) {
            return YearMonth.from(p.getPaidAt().atZone(ZoneOffset.UTC)).toString();
        }
        if (p.getCreatedAt() != null) {
            return YearMonth.from(p.getCreatedAt().atZone(ZoneOffset.UTC)).toString();
        }
        return YearMonth.now().toString();
    }

    private void requireFinance(UserPrincipal caller) {
        if (!FINANCE_ACCESS.contains(caller.role())) {
            throw new ForbiddenException("Finance portal access required");
        }
    }

    private void requireFinanceWrite(UserPrincipal caller) {
        requireFinance(caller);
        if (!EnumSet.of(Role.FINANCE, Role.ADMIN, Role.CEO).contains(caller.role())) {
            throw new ForbiddenException("Only finance officers and admins can post expenses");
        }
    }
}

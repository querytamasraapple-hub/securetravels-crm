package com.securetravels.crm.finance;

import com.securetravels.crm.booking.Booking;
import com.securetravels.crm.booking.BookingRepository;
import com.securetravels.crm.common.exception.BadRequestException;
import com.securetravels.crm.common.exception.ForbiddenException;
import com.securetravels.crm.common.exception.NotFoundException;
import com.securetravels.crm.common.util.PhoneUtils;
import com.securetravels.crm.common.util.XssSanitizer;
import com.securetravels.crm.finance.dto.FinanceAgingResponse;
import com.securetravels.crm.finance.dto.FinanceBudgetRequest;
import com.securetravels.crm.finance.dto.FinanceBudgetResponse;
import com.securetravels.crm.finance.dto.FinanceCashflowResponse;
import com.securetravels.crm.finance.dto.FinanceInvoiceLineRequest;
import com.securetravels.crm.finance.dto.FinanceInvoiceRequest;
import com.securetravels.crm.finance.dto.FinanceInvoiceResponse;
import com.securetravels.crm.finance.dto.FinanceOverviewResponse;
import com.securetravels.crm.finance.dto.FinancePayoutRequest;
import com.securetravels.crm.finance.dto.FinancePayoutResponse;
import com.securetravels.crm.finance.dto.FinanceTaxResponse;
import com.securetravels.crm.finance.dto.FinanceTripPnLResponse;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Advanced Finance ERP analytics + ledgers (aging, cashflow, GST, trip P&amp;L,
 * invoices, vendor payouts, budgets).
 */
@Service
public class FinanceExtendedService {

    private static final Set<Role> FINANCE_ACCESS = EnumSet.of(
            Role.FINANCE, Role.MANAGER, Role.ADMIN, Role.CEO);
    private static final Set<Role> FINANCE_WRITE = EnumSet.of(Role.FINANCE, Role.ADMIN, Role.CEO);
    private static final BigDecimal GST_RATE = new BigDecimal("0.05");
    private static final BigDecimal INPUT_CREDIT_RATE = new BigDecimal("0.05");

    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final FinanceExpenseRepository expenses;
    private final TripRepository trips;
    private final FinanceInvoiceRepository invoices;
    private final FinanceVendorPayoutRepository payouts;
    private final FinanceBudgetRepository budgets;

    public FinanceExtendedService(PaymentRepository payments, BookingRepository bookings,
                                  FinanceExpenseRepository expenses, TripRepository trips,
                                  FinanceInvoiceRepository invoices, FinanceVendorPayoutRepository payouts,
                                  FinanceBudgetRepository budgets) {
        this.payments = payments;
        this.bookings = bookings;
        this.expenses = expenses;
        this.trips = trips;
        this.invoices = invoices;
        this.payouts = payouts;
        this.budgets = budgets;
    }

    @Transactional(readOnly = true)
    public FinanceAgingResponse aging(UserPrincipal caller) {
        requireFinance(caller);
        LocalDate today = LocalDate.now();
        Map<UUID, Booking> bookingById = bookings.findAll().stream()
                .collect(Collectors.toMap(Booking::getId, b -> b, (a, b) -> a));

        BigDecimal current = BigDecimal.ZERO;
        BigDecimal d30 = BigDecimal.ZERO;
        BigDecimal d60 = BigDecimal.ZERO;
        BigDecimal d90 = BigDecimal.ZERO;
        BigDecimal d90p = BigDecimal.ZERO;
        List<FinanceAgingResponse.AgingLine> lines = new ArrayList<>();

        for (Payment p : payments.findAll()) {
            if (p.getStatus() != Payment.Status.PENDING && p.getStatus() != Payment.Status.OVERDUE
                    && p.getStatus() != Payment.Status.PARTIAL) {
                continue;
            }
            LocalDate due = p.getDueDate() != null ? p.getDueDate() : today;
            int days = (int) ChronoUnit.DAYS.between(due, today);
            if (days < 0) days = 0;
            String bucket;
            if (p.getDueDate() == null || !p.getDueDate().isBefore(today)) {
                bucket = "CURRENT";
                current = current.add(p.getAmount());
            } else if (days <= 30) {
                bucket = "1-30";
                d30 = d30.add(p.getAmount());
            } else if (days <= 60) {
                bucket = "31-60";
                d60 = d60.add(p.getAmount());
            } else if (days <= 90) {
                bucket = "61-90";
                d90 = d90.add(p.getAmount());
            } else {
                bucket = "90+";
                d90p = d90p.add(p.getAmount());
            }
            Booking b = bookingById.get(p.getBookingId());
            lines.add(new FinanceAgingResponse.AgingLine(
                    b != null ? b.getBookingRef() : p.getBookingId().toString().substring(0, 8),
                    p.getAmountType().name(),
                    p.getAmount(),
                    p.getDueDate() != null ? p.getDueDate().toString() : null,
                    days,
                    bucket,
                    p.getStatus().name()));
        }

        lines.sort(Comparator.comparingInt(FinanceAgingResponse.AgingLine::daysPastDue).reversed());
        BigDecimal total = current.add(d30).add(d60).add(d90).add(d90p);
        return new FinanceAgingResponse(current, d30, d60, d90, d90p, total, lines);
    }

    @Transactional(readOnly = true)
    public FinanceCashflowResponse cashflow(UserPrincipal caller, LocalDate from, LocalDate to) {
        requireFinance(caller);
        LocalDate rangeFrom = from != null ? from : LocalDate.now().minusDays(29);
        LocalDate rangeTo = to != null ? to : LocalDate.now();

        Map<String, BigDecimal> in = new LinkedHashMap<>();
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        for (LocalDate d = rangeFrom; !d.isAfter(rangeTo); d = d.plusDays(1)) {
            in.put(d.toString(), BigDecimal.ZERO);
            out.put(d.toString(), BigDecimal.ZERO);
        }

        for (Payment p : payments.findAll()) {
            if (p.getStatus() != Payment.Status.COMPLETED && p.getStatus() != Payment.Status.PARTIAL) continue;
            LocalDate d = paymentDate(p);
            if (d == null || d.isBefore(rangeFrom) || d.isAfter(rangeTo)) continue;
            in.merge(d.toString(), p.getAmount(), BigDecimal::add);
        }
        for (FinanceExpense e : expenses.findByExpenseDateBetweenOrderByExpenseDateDesc(rangeFrom, rangeTo)) {
            if (e.getStatus() != FinanceExpense.Status.POSTED) continue;
            out.merge(e.getExpenseDate().toString(), e.getAmount(), BigDecimal::add);
        }
        for (FinanceVendorPayout p : payouts.findAll()) {
            if (p.getStatus() != FinanceVendorPayout.Status.PAID) continue;
            LocalDate d = p.getPayoutDate();
            if (d.isBefore(rangeFrom) || d.isAfter(rangeTo)) continue;
            out.merge(d.toString(), p.getAmount(), BigDecimal::add);
        }

        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        List<FinanceCashflowResponse.DayPoint> daily = new ArrayList<>();
        for (String key : in.keySet()) {
            BigDecimal i = in.get(key);
            BigDecimal o = out.getOrDefault(key, BigDecimal.ZERO);
            totalIn = totalIn.add(i);
            totalOut = totalOut.add(o);
            daily.add(new FinanceCashflowResponse.DayPoint(key, i, o, i.subtract(o)));
        }
        return new FinanceCashflowResponse(rangeFrom.toString(), rangeTo.toString(),
                totalIn, totalOut, totalIn.subtract(totalOut), daily);
    }

    @Transactional(readOnly = true)
    public FinanceTaxResponse tax(UserPrincipal caller, LocalDate from, LocalDate to) {
        requireFinance(caller);
        LocalDate rangeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate rangeTo = to != null ? to : LocalDate.now();

        Map<UUID, Booking> bookingById = bookings.findAll().stream()
                .collect(Collectors.toMap(Booking::getId, b -> b, (a, b) -> a));
        Map<UUID, String> tripNames = trips.findAll().stream()
                .collect(Collectors.toMap(Trip::getId, Trip::getName, (a, b) -> a));
        Map<String, BigDecimal> byTrip = new LinkedHashMap<>();

        BigDecimal taxable = BigDecimal.ZERO;
        for (Payment p : payments.findAll()) {
            if (p.getStatus() != Payment.Status.COMPLETED && p.getStatus() != Payment.Status.PARTIAL) continue;
            LocalDate d = paymentDate(p);
            if (d == null || d.isBefore(rangeFrom) || d.isAfter(rangeTo)) continue;
            taxable = taxable.add(p.getAmount());
            Booking b = bookingById.get(p.getBookingId());
            if (b != null) {
                byTrip.merge(tripNames.getOrDefault(b.getTripId(), "Unknown"), p.getAmount(), BigDecimal::add);
            }
        }

        BigDecimal output = taxable.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expenseBase = expenses.findByExpenseDateBetweenOrderByExpenseDateDesc(rangeFrom, rangeTo).stream()
                .filter(e -> e.getStatus() == FinanceExpense.Status.POSTED)
                .map(FinanceExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal input = expenseBase.multiply(INPUT_CREDIT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = output.subtract(input);

        List<FinanceOverviewResponse.NamedAmount> tripTax = byTrip.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new FinanceOverviewResponse.NamedAmount(e.getKey(), e.getValue(), 0))
                .toList();

        return new FinanceTaxResponse(rangeFrom.toString(), rangeTo.toString(), taxable, output, input, net,
                new BigDecimal("5.0"), tripTax);
    }

    @Transactional(readOnly = true)
    public FinanceTripPnLResponse tripPnL(UserPrincipal caller) {
        requireFinance(caller);
        Map<UUID, String> tripNames = trips.findAll().stream()
                .collect(Collectors.toMap(Trip::getId, Trip::getName, (a, b) -> a));
        Map<UUID, Booking> bookingById = bookings.findAll().stream()
                .collect(Collectors.toMap(Booking::getId, b -> b, (a, b) -> a));

        Map<UUID, BigDecimal> rev = new LinkedHashMap<>();
        Map<UUID, Long> bookingCnt = new LinkedHashMap<>();
        for (Booking b : bookings.findAll()) {
            if (b.getStatus() == Booking.Status.CANCELLED) continue;
            bookingCnt.merge(b.getTripId(), 1L, Long::sum);
        }
        for (Payment p : payments.findAll()) {
            if (p.getStatus() != Payment.Status.COMPLETED && p.getStatus() != Payment.Status.PARTIAL) continue;
            Booking b = bookingById.get(p.getBookingId());
            if (b == null) continue;
            rev.merge(b.getTripId(), p.getAmount(), BigDecimal::add);
        }
        Map<UUID, BigDecimal> exp = new LinkedHashMap<>();
        for (FinanceExpense e : expenses.findAll()) {
            if (e.getStatus() != FinanceExpense.Status.POSTED || e.getTripId() == null) continue;
            exp.merge(e.getTripId(), e.getAmount(), BigDecimal::add);
        }
        Map<UUID, BigDecimal> pay = new LinkedHashMap<>();
        for (FinanceVendorPayout p : payouts.findAll()) {
            if (p.getStatus() == FinanceVendorPayout.Status.CANCELLED || p.getTripId() == null) continue;
            pay.merge(p.getTripId(), p.getAmount(), BigDecimal::add);
        }

        List<FinanceTripPnLResponse.TripRow> rows = new ArrayList<>();
        for (UUID tripId : tripNames.keySet()) {
            BigDecimal r = rev.getOrDefault(tripId, BigDecimal.ZERO);
            BigDecimal e = exp.getOrDefault(tripId, BigDecimal.ZERO);
            BigDecimal py = pay.getOrDefault(tripId, BigDecimal.ZERO);
            BigDecimal profit = r.subtract(e).subtract(py);
            BigDecimal margin = r.signum() == 0 ? BigDecimal.ZERO
                    : profit.multiply(BigDecimal.valueOf(100)).divide(r, 1, RoundingMode.HALF_UP);
            if (r.signum() == 0 && e.signum() == 0 && py.signum() == 0
                    && bookingCnt.getOrDefault(tripId, 0L) == 0) {
                continue;
            }
            rows.add(new FinanceTripPnLResponse.TripRow(
                    tripId, tripNames.get(tripId), bookingCnt.getOrDefault(tripId, 0L),
                    r, e, py, profit, margin));
        }
        rows.sort(Comparator.comparing(FinanceTripPnLResponse.TripRow::profit).reversed());
        return new FinanceTripPnLResponse(rows);
    }

    @Transactional(readOnly = true)
    public List<FinanceInvoiceResponse> listInvoices(UserPrincipal caller) {
        requireFinance(caller);
        LocalDate today = LocalDate.now();
        return invoices.findAllWithLines().stream()
                .map(i -> FinanceInvoiceResponse.from(i, displayStatus(i, today)))
                .toList();
    }

    @Transactional(readOnly = true)
    public FinanceInvoiceResponse getInvoice(UUID id, UserPrincipal caller) {
        requireFinance(caller);
        FinanceInvoice inv = invoices.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));
        return FinanceInvoiceResponse.from(inv, displayStatus(inv, LocalDate.now()));
    }

    @Transactional
    public FinanceInvoiceResponse createInvoice(FinanceInvoiceRequest request, UserPrincipal caller) {
        requireWrite(caller);
        String whatsapp = PhoneUtils.normalize(request.customerWhatsapp());
        if (whatsapp == null) {
            throw new BadRequestException("Valid Indian WhatsApp mobile is required (10 digits)");
        }

        String prefix = "TOH/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yy")) + "-"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("MM")) + "/";
        long seq = invoices.countByInvoiceNoStartingWith(prefix) + 1;
        String invoiceNo = prefix + String.format("%04d", seq);

        List<FinanceInvoiceLine> builtLines = new ArrayList<>();
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal gstRate = new BigDecimal("5.00");
        for (FinanceInvoiceLineRequest line : request.lines()) {
            BigDecimal qty = line.quantity();
            BigDecimal rate = line.rate();
            BigDecimal lineTaxable = qty.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            taxable = taxable.add(lineTaxable);
            if (line.gstRate() != null) gstRate = line.gstRate();
            builtLines.add(new FinanceInvoiceLine(
                    XssSanitizer.text(line.description()),
                    qty,
                    line.unit() != null ? line.unit() : "NOS",
                    rate,
                    lineTaxable,
                    line.hsnSac() != null ? line.hsnSac() : (request.sacCode() != null ? request.sacCode() : "998555"),
                    line.gstRate() != null ? line.gstRate() : new BigDecimal("5.00")));
        }

        FinanceInvoice.SupplyType supply = request.supplyType() != null
                ? request.supplyType() : FinanceInvoice.SupplyType.INTRA;
        String sellerStateCode = "05";
        String buyerStateCode = firstNonBlank(request.customerStateCode(), request.placeOfSupplyCode(), sellerStateCode);
        if (request.supplyType() == null
                && buyerStateCode != null
                && !buyerStateCode.equalsIgnoreCase(sellerStateCode)) {
            supply = FinanceInvoice.SupplyType.INTER;
        }

        BigDecimal cgstRate = BigDecimal.ZERO;
        BigDecimal sgstRate = BigDecimal.ZERO;
        BigDecimal igstRate = BigDecimal.ZERO;
        BigDecimal cgstAmt = BigDecimal.ZERO;
        BigDecimal sgstAmt = BigDecimal.ZERO;
        BigDecimal igstAmt = BigDecimal.ZERO;
        if (supply == FinanceInvoice.SupplyType.INTER) {
            igstRate = gstRate;
            igstAmt = taxable.multiply(igstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            cgstRate = gstRate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            sgstRate = cgstRate;
            cgstAmt = taxable.multiply(cgstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            sgstAmt = taxable.multiply(sgstRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        BigDecimal tax = cgstAmt.add(sgstAmt).add(igstAmt);
        BigDecimal total = taxable.add(tax);

        FinanceInvoice inv = new FinanceInvoice(
                invoiceNo,
                XssSanitizer.text(request.customerName()),
                request.issueDate(),
                request.dueDate(),
                taxable);
        inv.setBookingId(request.bookingId());
        inv.setCustomerEmail(XssSanitizer.text(request.customerEmail()));
        inv.setCustomerWhatsapp(whatsapp);
        inv.setCustomerPhone(PhoneUtils.normalize(request.customerPhone()) != null
                ? PhoneUtils.normalize(request.customerPhone()) : whatsapp);
        inv.setCustomerGstin(upper(XssSanitizer.text(request.customerGstin())));
        inv.setCustomerAddress(XssSanitizer.text(request.customerAddress()));
        inv.setCustomerState(firstNonBlank(XssSanitizer.text(request.customerState()), "Uttarakhand"));
        inv.setCustomerStateCode(buyerStateCode);
        inv.setPlaceOfSupply(firstNonBlank(XssSanitizer.text(request.placeOfSupply()), inv.getCustomerState()));
        inv.setPlaceOfSupplyCode(firstNonBlank(request.placeOfSupplyCode(), buyerStateCode));

        applySellerDefaults(inv);
        inv.setInvoiceType(request.invoiceType() != null ? request.invoiceType() : FinanceInvoice.InvoiceType.TAX_INVOICE);
        inv.setSupplyType(supply);
        inv.setReverseCharge(Boolean.TRUE.equals(request.reverseCharge()));
        inv.setSacCode(request.sacCode() != null ? request.sacCode() : "998555");
        inv.setCgstRate(cgstRate);
        inv.setSgstRate(sgstRate);
        inv.setIgstRate(igstRate);
        inv.setCgstAmount(cgstAmt);
        inv.setSgstAmount(sgstAmt);
        inv.setIgstAmount(igstAmt);
        inv.setTaxAmount(tax);
        inv.setTotalAmount(total);
        inv.setAmountInWords(IndianAmountInWords.of(total));
        inv.setStatus(request.status() != null ? request.status() : FinanceInvoice.Status.SENT);
        inv.setNotes(XssSanitizer.text(request.notes()));
        inv.setDeclaration(firstNonBlank(
                XssSanitizer.text(request.declaration()),
                "We declare that this invoice shows the actual price of the services described and that all particulars are true and correct."));
        inv.setRecordedBy(caller.id());
        inv.clearAndAddLines(builtLines);

        boolean queueWa = request.queueWhatsapp() == null || Boolean.TRUE.equals(request.queueWhatsapp());
        if (queueWa) {
            inv.setWhatsappDeliveryStatus(FinanceInvoice.WhatsappDeliveryStatus.QUEUED);
            inv.setWhatsappQueuedAt(Instant.now());
        } else {
            inv.setWhatsappDeliveryStatus(FinanceInvoice.WhatsappDeliveryStatus.PENDING);
        }

        return FinanceInvoiceResponse.from(invoices.save(inv));
    }

    @Transactional
    public FinanceInvoiceResponse updateInvoiceStatus(UUID id, FinanceInvoice.Status status, UserPrincipal caller) {
        requireWrite(caller);
        FinanceInvoice inv = invoices.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));
        inv.setStatus(status);
        return FinanceInvoiceResponse.from(invoices.save(inv));
    }

    /**
     * Queues the GST invoice for WhatsApp delivery (automation hook).
     * Actual Meta/WhatsApp Business send will pick QUEUED rows.
     */
    @Transactional
    public FinanceInvoiceResponse queueInvoiceWhatsapp(UUID id, UserPrincipal caller) {
        requireWrite(caller);
        FinanceInvoice inv = invoices.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Invoice not found: " + id));
        if (inv.getCustomerWhatsapp() == null || inv.getCustomerWhatsapp().isBlank()) {
            throw new BadRequestException("Invoice has no WhatsApp number — add customer WhatsApp first");
        }
        inv.setWhatsappDeliveryStatus(FinanceInvoice.WhatsappDeliveryStatus.QUEUED);
        inv.setWhatsappQueuedAt(Instant.now());
        inv.setWhatsappLastError(null);
        return FinanceInvoiceResponse.from(invoices.save(inv));
    }

    private static FinanceInvoice.Status displayStatus(FinanceInvoice i, LocalDate today) {
        FinanceInvoice.Status status = i.getStatus();
        if ((status == FinanceInvoice.Status.SENT || status == FinanceInvoice.Status.PARTIALLY_PAID)
                && i.getDueDate().isBefore(today)) {
            return FinanceInvoice.Status.OVERDUE;
        }
        return status;
    }

    private static void applySellerDefaults(FinanceInvoice inv) {
        inv.setSellerLegalName("Trip On Himalaya Adventures");
        inv.setSellerTradeName("Trip On Himalaya");
        inv.setSellerGstin("05AABCT0000A1Z5");
        inv.setSellerAddress("Near Mall Road, Rishikesh, Uttarakhand 249201, India");
        inv.setSellerState("Uttarakhand");
        inv.setSellerStateCode("05");
        inv.setSellerPhone("9876000000");
        inv.setSellerEmail("accounts@triponhimalaya.com");
        inv.setSellerBankName("HDFC Bank");
        inv.setSellerBankAccount("50200012345678");
        inv.setSellerBankIfsc("HDFC0001234");
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static String upper(String v) {
        return v == null ? null : v.trim().toUpperCase();
    }

    @Transactional(readOnly = true)
    public List<FinancePayoutResponse> listPayouts(UserPrincipal caller) {
        requireFinance(caller);
        Map<UUID, String> tripNames = trips.findAll().stream()
                .collect(Collectors.toMap(Trip::getId, Trip::getName, (a, b) -> a));
        return payouts.findAllByOrderByPayoutDateDesc().stream()
                .map(p -> FinancePayoutResponse.from(p,
                        p.getTripId() == null ? null : tripNames.get(p.getTripId())))
                .toList();
    }

    @Transactional
    public FinancePayoutResponse createPayout(FinancePayoutRequest request, UserPrincipal caller) {
        requireWrite(caller);
        FinanceVendorPayout p = new FinanceVendorPayout(
                XssSanitizer.text(request.vendorName()),
                request.category(),
                request.amount(),
                request.payoutDate());
        p.setTripId(request.tripId());
        p.setDueDate(request.dueDate());
        p.setPaymentMethod(request.paymentMethod() != null ? request.paymentMethod()
                : FinanceVendorPayout.PaymentMethod.BANK_TRANSFER);
        p.setStatus(request.status() != null ? request.status() : FinanceVendorPayout.Status.SCHEDULED);
        p.setReferenceNo(XssSanitizer.text(request.referenceNo()));
        p.setNotes(XssSanitizer.text(request.notes()));
        p.setRecordedBy(caller.id());
        FinanceVendorPayout saved = payouts.save(p);
        String tripName = saved.getTripId() == null ? null
                : trips.findById(saved.getTripId()).map(Trip::getName).orElse(null);
        return FinancePayoutResponse.from(saved, tripName);
    }

    @Transactional
    public FinancePayoutResponse updatePayoutStatus(UUID id, FinanceVendorPayout.Status status, UserPrincipal caller) {
        requireWrite(caller);
        FinanceVendorPayout p = payouts.findById(id)
                .orElseThrow(() -> new NotFoundException("Payout not found: " + id));
        p.setStatus(status);
        String tripName = p.getTripId() == null ? null
                : trips.findById(p.getTripId()).map(Trip::getName).orElse(null);
        return FinancePayoutResponse.from(payouts.save(p), tripName);
    }

    @Transactional(readOnly = true)
    public FinanceBudgetResponse budgets(UserPrincipal caller, String periodMonth) {
        requireFinance(caller);
        String month = periodMonth != null ? periodMonth : YearMonth.now().toString();
        LocalDate from = YearMonth.parse(month).atDay(1);
        LocalDate to = YearMonth.parse(month).atEndOfMonth();

        Map<String, BigDecimal> actual = new LinkedHashMap<>();
        for (FinanceExpense e : expenses.findByExpenseDateBetweenOrderByExpenseDateDesc(from, to)) {
            if (e.getStatus() != FinanceExpense.Status.POSTED) continue;
            actual.merge(e.getCategory().name(), e.getAmount(), BigDecimal::add);
        }

        List<FinanceBudget> rows = budgets.findByPeriodMonthOrderByCategoryAsc(month);
        List<FinanceBudgetResponse.BudgetLine> lines = new ArrayList<>();
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;

        if (rows.isEmpty()) {
            for (Map.Entry<String, BigDecimal> e : actual.entrySet()) {
                totalActual = totalActual.add(e.getValue());
                lines.add(new FinanceBudgetResponse.BudgetLine(
                        e.getKey(), BigDecimal.ZERO, e.getValue(), e.getValue().negate(), BigDecimal.ZERO));
            }
        } else {
            for (FinanceBudget b : rows) {
                BigDecimal a = actual.getOrDefault(b.getCategory(), BigDecimal.ZERO);
                totalBudget = totalBudget.add(b.getBudgetAmount());
                totalActual = totalActual.add(a);
                BigDecimal used = b.getBudgetAmount().signum() == 0 ? BigDecimal.ZERO
                        : a.multiply(BigDecimal.valueOf(100)).divide(b.getBudgetAmount(), 1, RoundingMode.HALF_UP);
                lines.add(new FinanceBudgetResponse.BudgetLine(
                        b.getCategory(), b.getBudgetAmount(), a, b.getBudgetAmount().subtract(a), used));
            }
            for (Map.Entry<String, BigDecimal> e : actual.entrySet()) {
                boolean known = rows.stream().anyMatch(r -> r.getCategory().equals(e.getKey()));
                if (!known) {
                    totalActual = totalActual.add(e.getValue());
                    lines.add(new FinanceBudgetResponse.BudgetLine(
                            e.getKey(), BigDecimal.ZERO, e.getValue(), e.getValue().negate(), BigDecimal.ZERO));
                }
            }
        }

        return new FinanceBudgetResponse(month, lines, totalBudget, totalActual, totalBudget.subtract(totalActual));
    }

    @Transactional
    public FinanceBudgetResponse upsertBudget(FinanceBudgetRequest request, UserPrincipal caller) {
        requireWrite(caller);
        FinanceBudget row = budgets.findByPeriodMonthAndCategory(request.periodMonth(), request.category())
                .orElseGet(() -> new FinanceBudget(request.periodMonth(), request.category(), request.budgetAmount()));
        row.setBudgetAmount(request.budgetAmount());
        row.setNotes(XssSanitizer.text(request.notes()));
        row.setRecordedBy(caller.id());
        budgets.save(row);
        return budgets(caller, request.periodMonth());
    }

    private static LocalDate paymentDate(Payment p) {
        if (p.getPaidAt() != null) return p.getPaidAt().atZone(ZoneOffset.UTC).toLocalDate();
        if (p.getCreatedAt() != null) return p.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
        return null;
    }

    private void requireFinance(UserPrincipal caller) {
        if (!FINANCE_ACCESS.contains(caller.role())) {
            throw new ForbiddenException("Finance portal access required");
        }
    }

    private void requireWrite(UserPrincipal caller) {
        requireFinance(caller);
        if (!FINANCE_WRITE.contains(caller.role())) {
            throw new ForbiddenException("Only finance officers and admins can mutate finance ledgers");
        }
    }
}

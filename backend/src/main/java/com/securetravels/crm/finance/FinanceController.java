package com.securetravels.crm.finance;

import com.securetravels.crm.common.security.CurrentUser;
import com.securetravels.crm.finance.dto.FinanceAgingResponse;
import com.securetravels.crm.finance.dto.FinanceBudgetRequest;
import com.securetravels.crm.finance.dto.FinanceBudgetResponse;
import com.securetravels.crm.finance.dto.FinanceCashflowResponse;
import com.securetravels.crm.finance.dto.FinanceExpenseRequest;
import com.securetravels.crm.finance.dto.FinanceExpenseResponse;
import com.securetravels.crm.finance.dto.FinanceInvoiceRequest;
import com.securetravels.crm.finance.dto.FinanceInvoiceResponse;
import com.securetravels.crm.finance.dto.FinanceOverviewResponse;
import com.securetravels.crm.finance.dto.FinancePayoutRequest;
import com.securetravels.crm.finance.dto.FinancePayoutResponse;
import com.securetravels.crm.finance.dto.FinancePnLResponse;
import com.securetravels.crm.finance.dto.FinanceTaxResponse;
import com.securetravels.crm.finance.dto.FinanceTripPnLResponse;
import com.securetravels.crm.user.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance")
@PreAuthorize("hasAnyRole('FINANCE', 'MANAGER', 'ADMIN', 'CEO')")
public class FinanceController {

    private final FinanceService finance;
    private final FinanceExtendedService extended;

    public FinanceController(FinanceService finance, FinanceExtendedService extended) {
        this.finance = finance;
        this.extended = extended;
    }

    @Operation(summary = "Company finance overview",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/overview", produces = MediaType.APPLICATION_JSON_VALUE)
    public FinanceOverviewResponse overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @CurrentUser UserPrincipal caller) {
        return finance.overview(caller, from, to);
    }

    @Operation(summary = "Profit & loss",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/pnl", produces = MediaType.APPLICATION_JSON_VALUE)
    public FinancePnLResponse pnl(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @CurrentUser UserPrincipal caller) {
        return finance.pnl(caller, from, to);
    }

    @Operation(summary = "AR aging buckets",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/aging", produces = MediaType.APPLICATION_JSON_VALUE)
    public FinanceAgingResponse aging(@CurrentUser UserPrincipal caller) {
        return extended.aging(caller);
    }

    @Operation(summary = "Daily cashflow (inflows vs outflows)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/cashflow", produces = MediaType.APPLICATION_JSON_VALUE)
    public FinanceCashflowResponse cashflow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @CurrentUser UserPrincipal caller) {
        return extended.cashflow(caller, from, to);
    }

    @Operation(summary = "GST / tax summary (5% tour package default)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/tax", produces = MediaType.APPLICATION_JSON_VALUE)
    public FinanceTaxResponse tax(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @CurrentUser UserPrincipal caller) {
        return extended.tax(caller, from, to);
    }

    @Operation(summary = "Per-trek profitability",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/trip-pnl", produces = MediaType.APPLICATION_JSON_VALUE)
    public FinanceTripPnLResponse tripPnL(@CurrentUser UserPrincipal caller) {
        return extended.tripPnL(caller);
    }

    @GetMapping(value = "/expenses", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FinanceExpenseResponse> listExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @CurrentUser UserPrincipal caller) {
        return finance.listExpenses(caller, from, to);
    }

    @PostMapping(value = "/expenses", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN', 'CEO')")
    public FinanceExpenseResponse createExpense(@Valid @RequestBody FinanceExpenseRequest request,
                                                @CurrentUser UserPrincipal caller) {
        return finance.createExpense(request, caller);
    }

    @PostMapping(value = "/expenses/{id}/void", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN', 'CEO')")
    public FinanceExpenseResponse voidExpense(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        return finance.voidExpense(id, caller);
    }

    @GetMapping(value = "/invoices", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FinanceInvoiceResponse> listInvoices(@CurrentUser UserPrincipal caller) {
        return extended.listInvoices(caller);
    }

    @GetMapping(value = "/invoices/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FinanceInvoiceResponse getInvoice(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        return extended.getInvoice(id, caller);
    }

    @PostMapping(value = "/invoices", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN', 'CEO')")
    public FinanceInvoiceResponse createInvoice(@Valid @RequestBody FinanceInvoiceRequest request,
                                                @CurrentUser UserPrincipal caller) {
        return extended.createInvoice(request, caller);
    }

    @PostMapping(value = "/invoices/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN', 'CEO')")
    public FinanceInvoiceResponse invoiceStatus(@PathVariable UUID id,
                                                @RequestBody Map<String, String> body,
                                                @CurrentUser UserPrincipal caller) {
        return extended.updateInvoiceStatus(id, FinanceInvoice.Status.valueOf(body.get("status")), caller);
    }

    @Operation(summary = "Queue GST invoice for WhatsApp automation delivery",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/invoices/{id}/queue-whatsapp", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN', 'CEO')")
    public FinanceInvoiceResponse queueWhatsapp(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        return extended.queueInvoiceWhatsapp(id, caller);
    }

    @GetMapping(value = "/payouts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FinancePayoutResponse> listPayouts(@CurrentUser UserPrincipal caller) {
        return extended.listPayouts(caller);
    }

    @PostMapping(value = "/payouts", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN', 'CEO')")
    public FinancePayoutResponse createPayout(@Valid @RequestBody FinancePayoutRequest request,
                                              @CurrentUser UserPrincipal caller) {
        return extended.createPayout(request, caller);
    }

    @PostMapping(value = "/payouts/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN', 'CEO')")
    public FinancePayoutResponse payoutStatus(@PathVariable UUID id,
                                              @RequestBody Map<String, String> body,
                                              @CurrentUser UserPrincipal caller) {
        return extended.updatePayoutStatus(id, FinanceVendorPayout.Status.valueOf(body.get("status")), caller);
    }

    @GetMapping(value = "/budgets", produces = MediaType.APPLICATION_JSON_VALUE)
    public FinanceBudgetResponse budgets(@RequestParam(required = false) String month,
                                         @CurrentUser UserPrincipal caller) {
        return extended.budgets(caller, month);
    }

    @PostMapping(value = "/budgets", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN', 'CEO')")
    public FinanceBudgetResponse upsertBudget(@Valid @RequestBody FinanceBudgetRequest request,
                                              @CurrentUser UserPrincipal caller) {
        return extended.upsertBudget(request, caller);
    }
}

package com.securetravels.crm.accounts;

import com.securetravels.crm.accounts.dto.AccountCreateRequest;
import com.securetravels.crm.accounts.dto.AccountDetailResponse;
import com.securetravels.crm.accounts.dto.AccountResponse;
import com.securetravels.crm.accounts.dto.AccountUpdateRequest;
import com.securetravels.crm.accounts.dto.CommissionPayableResponse;
import com.securetravels.crm.accounts.dto.InvoiceResponse;
import com.securetravels.crm.accounts.dto.PayableSettleRequest;
import com.securetravels.crm.common.security.CurrentUser;
import com.securetravels.crm.user.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Phase 7 Module 1 — account records. Reads are authenticated; writes and
 * the financial views (360 detail, payables, invoices) are manager-and-up
 * (enforced in the service).
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accounts;
    private final AccountCommissionPayableService payables;
    private final AccountInvoiceService invoices;

    public AccountController(AccountService accounts, AccountCommissionPayableService payables,
                             AccountInvoiceService invoices) {
        this.accounts = accounts;
        this.payables = payables;
        this.invoices = invoices;
    }

    @Operation(summary = "List accounts (optional type filter)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public List<AccountResponse> list(@RequestParam(required = false) Account.AccountType type,
                                      @RequestParam(defaultValue = "true") boolean active,
                                      @CurrentUser UserPrincipal caller) {
        return accounts.list(type, active, caller);
    }

    @Operation(summary = "Get an account", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public AccountResponse get(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        return accounts.get(id, caller);
    }

    @Operation(summary = "Account 360: aggregates, recent bookings, payables (managers)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/{id}/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public AccountDetailResponse detail(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        return accounts.detail(id, caller);
    }

    @Operation(summary = "Create an account (managers)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public AccountResponse create(@Valid @RequestBody AccountCreateRequest request,
                                  @CurrentUser UserPrincipal caller) {
        return accounts.create(request, caller);
    }

    @Operation(summary = "Update an account (managers)", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public AccountResponse update(@PathVariable UUID id, @Valid @RequestBody AccountUpdateRequest request,
                                  @CurrentUser UserPrincipal caller) {
        return accounts.update(id, request, caller);
    }

    @Operation(summary = "Commission payables for an account (managers)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/{id}/payables", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public List<CommissionPayableResponse> payables(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        return payables.listForAccount(id, caller);
    }

    @Operation(summary = "Settle an OPEN payable (managers)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/{id}/payables/{payableId}/settle", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public void settle(@PathVariable UUID id, @PathVariable UUID payableId,
                       @RequestBody(required = false) @Valid PayableSettleRequest request,
                       @CurrentUser UserPrincipal caller) {
        payables.markPaid(payableId, caller,
                request == null || request.paidRef() == null ? null : request.paidRef(), Instant.now());
    }

    @Operation(summary = "Invoices for an account (managers)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/{id}/invoices", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public List<InvoiceResponse> invoices(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        return invoices.listForAccount(id, caller);
    }

    @Operation(summary = "Get an invoice", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/invoices/{invoiceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public InvoiceResponse invoice(@PathVariable UUID invoiceId, @CurrentUser UserPrincipal caller) {
        return invoices.get(invoiceId, caller);
    }
}
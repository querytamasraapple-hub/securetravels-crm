package com.securetravels.crm.commission;

import com.securetravels.crm.accounts.AccountRepository;
import com.securetravels.crm.commission.dto.CommissionPlanAssignmentResponse;
import com.securetravels.crm.commission.dto.CommissionPlanAssignRequest;
import com.securetravels.crm.commission.dto.CommissionPlanBasis;
import com.securetravels.crm.commission.dto.CommissionPlanCreateRequest;
import com.securetravels.crm.commission.dto.CommissionPlanMethod;
import com.securetravels.crm.commission.dto.CommissionPlanResponse;
import com.securetravels.crm.commission.dto.CommissionPlanUpdateRequest;
import com.securetravels.crm.commission.dto.CommissionQuoteRequest;
import com.securetravels.crm.commission.dto.CommissionQuoteResponse;
import com.securetravels.crm.common.exception.NotFoundException;
import com.securetravels.crm.common.security.CurrentUser;
import com.securetravels.crm.user.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Phase 7 Module 4 — partner commission plans. Every endpoint is
 * manager-and-up: a plan is a commercial price paid to a partner, so this is
 * not part of the sales-facing surface (unlike pipeline stages, which every
 * user reads to build a forecast). Role enforcement lives in
 * {@link CommissionPlanService} so the rules hold however the service is
 * reached.
 */
@RestController
@RequestMapping("/api/commission-plans")
public class CommissionPlanController {

    private final CommissionPlanService commissionPlans;
    private final AccountRepository accounts;

    public CommissionPlanController(CommissionPlanService commissionPlans, AccountRepository accounts) {
        this.commissionPlans = commissionPlans;
        this.accounts = accounts;
    }

    @Operation(summary = "List commission plans (managers)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public List<CommissionPlanResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive,
                                             @CurrentUser UserPrincipal caller) {
        return commissionPlans.list(includeInactive, caller);
    }

    @Operation(summary = "Get a commission plan (managers)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public CommissionPlanResponse get(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        return commissionPlans.get(id, caller);
    }

    @Operation(summary = "Create a commission plan (managers)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public CommissionPlanResponse create(@Valid @RequestBody CommissionPlanCreateRequest request,
                                         @CurrentUser UserPrincipal caller) {
        return commissionPlans.create(request, caller);
    }

    @Operation(summary = "Update a commission plan (managers)", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public CommissionPlanResponse update(@PathVariable UUID id,
                                         @Valid @RequestBody CommissionPlanUpdateRequest request,
                                         @CurrentUser UserPrincipal caller) {
        return commissionPlans.update(id, request, caller);
    }

    @Operation(summary = "Delete an unused commission plan (managers)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void delete(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        commissionPlans.delete(id, caller);
    }

    @Operation(summary = "Assign a plan to an account (managers)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/accounts/{accountId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public CommissionPlanAssignmentResponse assign(@PathVariable UUID accountId,
                                                   @Valid @RequestBody CommissionPlanAssignRequest request,
                                                   @CurrentUser UserPrincipal caller) {
        return commissionPlans.assign(accountId, request, caller);
    }

    @Operation(summary = "Remove an account's plan assignment (managers)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping(value = "/accounts/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void unassign(@PathVariable UUID accountId, @CurrentUser UserPrincipal caller) {
        commissionPlans.unassign(accountId, caller);
    }

    @Operation(summary = "An account's active plan assignment (managers)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/accounts/{accountId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public CommissionPlanAssignmentResponse assignment(@PathVariable UUID accountId,
                                                       @CurrentUser UserPrincipal caller) {
        if (!accounts.existsById(accountId)) {
            throw new NotFoundException("Account not found: " + accountId);
        }
        return commissionPlans.assignmentFor(accountId, caller);
    }

    @Operation(summary = "Preview what an account's terms would pay on these amounts (managers)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/accounts/{accountId}/quote", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public CommissionQuoteResponse quote(@PathVariable UUID accountId,
                                         @Valid @RequestBody CommissionQuoteRequest request,
                                         @CurrentUser UserPrincipal caller) {
        if (!accounts.existsById(accountId)) {
            throw new NotFoundException("Account not found: " + accountId);
        }
        CommissionQuote computed = commissionPlans.quotePreview(accountId,
                request.grossAmount(), request.discountAmount(), request.taxAmount(), caller);
        return new CommissionQuoteResponse(accountId, computed.planId(), computed.planKey(),
                CommissionPlanBasis.valueOf(computed.basis().name()),
                CommissionPlanMethod.valueOf(computed.method().name()),
                computed.basisAmount(), computed.commissionAmount(), computed.appliedRatePercent(),
                computed.matchedTierFrom(), computed.matchedTierTo(),
                computed.belowThreshold(), computed.minSalesThreshold());
    }
}

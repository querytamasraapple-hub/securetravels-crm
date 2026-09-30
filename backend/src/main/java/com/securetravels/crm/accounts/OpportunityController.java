package com.securetravels.crm.accounts;

import com.securetravels.crm.accounts.dto.ForecastResponse;
import com.securetravels.crm.accounts.dto.OpportunityCloseRequest;
import com.securetravels.crm.accounts.dto.OpportunityCreateRequest;
import com.securetravels.crm.accounts.dto.OpportunityMoveRequest;
import com.securetravels.crm.accounts.dto.OpportunityResponse;
import com.securetravels.crm.common.security.CurrentUser;
import com.securetravels.crm.user.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Phase 7 Module 3 — opportunities and the revenue forecast. Reading is
 * scoped to the caller's ownership in the service; moves and closes are
 * owner-or-manager; creation is sales-and-up.
 */
@RestController
@RequestMapping("/api")
public class OpportunityController {

    private final OpportunityService opportunities;

    public OpportunityController(OpportunityService opportunities) {
        this.opportunities = opportunities;
    }

    @Operation(summary = "List opportunities (scoped to caller)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/opportunities", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public List<OpportunityResponse> list(@RequestParam(required = false) UUID leadId,
                                          @RequestParam(required = false) UUID ownerId,
                                          @RequestParam(required = false) String stage,
                                          @RequestParam(required = false) Opportunity.Status status,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                          @CurrentUser UserPrincipal caller) {
        return opportunities.list(leadId, ownerId, stage, status, from, to, caller);
    }

    @Operation(summary = "Get an opportunity", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/opportunities/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public OpportunityResponse get(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        return opportunities.get(id, caller);
    }

    @Operation(summary = "Create an opportunity for a lead (sales+)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/opportunities", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public OpportunityResponse create(@Valid @RequestBody OpportunityCreateRequest request,
                                      @CurrentUser UserPrincipal caller) {
        return opportunities.create(request, caller);
    }

    @Operation(summary = "Move an OPEN opportunity to another stage (owner or manager)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping(value = "/opportunities/{id}/stage", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public OpportunityResponse moveStage(@PathVariable UUID id,
                                         @Valid @RequestBody OpportunityMoveRequest request,
                                         @CurrentUser UserPrincipal caller) {
        return opportunities.moveStage(id, request, caller);
    }

    @Operation(summary = "Close an OPEN opportunity as WON or LOST (owner or manager)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(value = "/opportunities/{id}/close", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public OpportunityResponse close(@PathVariable UUID id,
                                     @Valid @RequestBody OpportunityCloseRequest request,
                                     @CurrentUser UserPrincipal caller) {
        return opportunities.close(id, request, caller);
    }

    @Operation(summary = "Revenue forecast over [from, to) on expected_date",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/forecast", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ForecastResponse forecast(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @CurrentUser UserPrincipal caller) {
        return opportunities.forecast(from, to, caller);
    }
}
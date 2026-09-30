package com.securetravels.crm.accounts;

import com.securetravels.crm.accounts.dto.PipelineStageCreateRequest;
import com.securetravels.crm.accounts.dto.PipelineStageResponse;
import com.securetravels.crm.accounts.dto.PipelineStageUpdateRequest;
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
 * Phase 7 Module 2 — configurable pipeline stages. Listing (active stages) is
 * open to any authenticated user; manage operations and inactive-stage
 * listing are manager-and-up (enforced in the service).
 */
@RestController
@RequestMapping("/api/pipeline-stages")
public class PipelineStageController {

    private final PipelineStageService stages;

    public PipelineStageController(PipelineStageService stages) {
        this.stages = stages;
    }

    @Operation(summary = "List active pipeline stages", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public List<PipelineStageResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive,
                                            @CurrentUser UserPrincipal caller) {
        return stages.list(includeInactive, caller);
    }

    @Operation(summary = "Get a pipeline stage", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public PipelineStageResponse get(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        return stages.get(id, caller);
    }

    @Operation(summary = "Create a pipeline stage (managers)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public PipelineStageResponse create(@Valid @RequestBody PipelineStageCreateRequest request,
                                        @CurrentUser UserPrincipal caller) {
        return stages.create(request, caller);
    }

    @Operation(summary = "Update a pipeline stage (managers)", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public PipelineStageResponse update(@PathVariable UUID id,
                                        @Valid @RequestBody PipelineStageUpdateRequest request,
                                        @CurrentUser UserPrincipal caller) {
        return stages.update(id, request, caller);
    }

    @Operation(summary = "Delete a pipeline stage (managers)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void delete(@PathVariable UUID id, @CurrentUser UserPrincipal caller) {
        stages.delete(id, caller);
    }
}
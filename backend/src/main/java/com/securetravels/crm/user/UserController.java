package com.securetravels.crm.user;

import com.securetravels.crm.common.security.CurrentUser;
import com.securetravels.crm.user.dto.TeamUserCreateRequest;
import com.securetravels.crm.user.dto.TeamUserResponse;
import com.securetravels.crm.user.dto.TeamUserUpdateRequest;
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

import java.util.List;
import java.util.UUID;

/** Team directory + legacy sales-picker used by targets. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final TeamUserService teamUsers;

    public UserController(UserRepository userRepository, TeamUserService teamUsers) {
        this.userRepository = userRepository;
        this.teamUsers = teamUsers;
    }

    public record SalesUser(UUID id, String fullName, String email) {}

    @Operation(summary = "List SALES users (for target assignment)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'CEO')")
    public List<SalesUser> sales() {
        return userRepository.findAllByRoleOrderByCreatedAtAsc(Role.SALES).stream()
                .map(u -> new SalesUser(u.getId(), u.getFullName(), u.getEmail()))
                .toList();
    }

    @Operation(summary = "List team users (managers+)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'CEO')")
    public List<TeamUserResponse> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @CurrentUser UserPrincipal caller) {
        return teamUsers.list(caller, role, active);
    }

    @Operation(summary = "Create a team user",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'CEO')")
    public TeamUserResponse create(@Valid @RequestBody TeamUserCreateRequest request,
                                   @CurrentUser UserPrincipal caller) {
        return teamUsers.create(request, caller);
    }

    @Operation(summary = "Update a team user (role, active, password, profile)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'CEO')")
    public TeamUserResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody TeamUserUpdateRequest request,
                                   @CurrentUser UserPrincipal caller) {
        return teamUsers.update(id, request, caller);
    }
}

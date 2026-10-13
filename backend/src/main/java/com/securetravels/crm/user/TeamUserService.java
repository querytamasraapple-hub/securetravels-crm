package com.securetravels.crm.user;

import com.securetravels.crm.common.exception.BadRequestException;
import com.securetravels.crm.common.exception.ConflictException;
import com.securetravels.crm.common.exception.ForbiddenException;
import com.securetravels.crm.common.exception.NotFoundException;
import com.securetravels.crm.user.dto.TeamUserCreateRequest;
import com.securetravels.crm.user.dto.TeamUserResponse;
import com.securetravels.crm.user.dto.TeamUserUpdateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Team directory for managers+. Role assignment is scoped:
 * <ul>
 *   <li>MANAGER — may create/edit SALES and OPS only</li>
 *   <li>ADMIN — any role except CEO</li>
 *   <li>CEO — any role</li>
 * </ul>
 */
@Service
public class TeamUserService {

    private static final Set<Role> DIRECTORY = EnumSet.of(Role.MANAGER, Role.ADMIN, Role.CEO);
    private static final Set<Role> MANAGER_ASSIGNABLE = EnumSet.of(Role.SALES, Role.OPS);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public TeamUserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<TeamUserResponse> list(UserPrincipal caller, Role roleFilter, Boolean activeFilter) {
        requireDirectory(caller);
        return users.findAllByOrderByFullNameAsc().stream()
                .filter(u -> roleFilter == null || u.getRole() == roleFilter)
                .filter(u -> activeFilter == null || u.isActive() == activeFilter)
                .map(TeamUserResponse::from)
                .toList();
    }

    @Transactional
    public TeamUserResponse create(TeamUserCreateRequest request, UserPrincipal caller) {
        requireDirectory(caller);
        assertCanAssign(caller, request.role(), null);

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with this email already exists");
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.role(),
                normalizePhone(request.phone()));
        return TeamUserResponse.from(users.save(user));
    }

    @Transactional
    public TeamUserResponse update(UUID id, TeamUserUpdateRequest request, UserPrincipal caller) {
        requireDirectory(caller);
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));

        // Must be allowed to manage the user's *current* role, and the new role if changing.
        assertCanAssign(caller, user.getRole(), user);
        if (request.role() != null && request.role() != user.getRole()) {
            assertCanAssign(caller, request.role(), user);
            if (user.getId().equals(caller.id())) {
                throw new BadRequestException("You cannot change your own role");
            }
            user.setRole(request.role());
        }

        if (request.fullName() != null) {
            String name = request.fullName().trim();
            if (name.isEmpty()) {
                throw new BadRequestException("fullName cannot be blank");
            }
            user.setFullName(name);
        }
        if (request.phone() != null) {
            user.setPhone(normalizePhone(request.phone()));
        }
        if (request.active() != null) {
            if (user.getId().equals(caller.id()) && !request.active()) {
                throw new BadRequestException("You cannot deactivate your own account");
            }
            user.setActive(request.active());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return TeamUserResponse.from(users.save(user));
    }

    private void requireDirectory(UserPrincipal caller) {
        if (!DIRECTORY.contains(caller.role())) {
            throw new ForbiddenException("Only managers and above can manage the team directory");
        }
    }

    /**
     * @param existing the user being edited (null on create); used to block ADMIN from
     *                 touching CEO accounts.
     */
    private void assertCanAssign(UserPrincipal caller, Role targetRole, User existing) {
        if (existing != null && existing.getRole() == Role.CEO && caller.role() != Role.CEO) {
            throw new ForbiddenException("Only the CEO can manage CEO accounts");
        }
        if (targetRole == Role.CEO && caller.role() != Role.CEO) {
            throw new ForbiddenException("Only the CEO can assign the CEO role");
        }
        if (caller.role() == Role.MANAGER && !MANAGER_ASSIGNABLE.contains(targetRole)) {
            throw new ForbiddenException("Managers can only assign SALES or OPS roles");
        }
    }

    /** Strip spaces / +91 / leading 0 → canonical 10-digit mobile, or null if blank. */
    static String normalizePhone(String value) {
        if (value == null) return null;
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        }
        if (digits.startsWith("0") && digits.length() == 11) {
            digits = digits.substring(1);
        }
        return digits;
    }
}

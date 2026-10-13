package com.securetravels.crm.user.dto;

import com.securetravels.crm.user.Role;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Partial update — null fields are left unchanged. */
public record TeamUserUpdateRequest(
        @Size(max = 120, message = "fullName too long")
        String fullName,

        Role role,

        @Pattern(regexp = "^(?:\\+?91[- ]?)?0?[6-9](?:[ -]?\\d){9}$",
                message = "phone must be a valid 10-digit Indian mobile (e.g. 9876543210)")
        String phone,

        Boolean active,

        @Size(min = 8, max = 72, message = "password must be 8–72 characters")
        String password
) {
}

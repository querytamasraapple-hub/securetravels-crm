package com.securetravels.crm.user.dto;

import com.securetravels.crm.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TeamUserCreateRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 255)
        String email,

        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName too long")
        String fullName,

        @NotNull(message = "role is required")
        Role role,

        // Optional leading 0 / +91; digits must resolve to a 10-digit mobile starting 6–9.
        @Pattern(regexp = "^(?:\\+?91[- ]?)?0?[6-9](?:[ -]?\\d){9}$",
                message = "phone must be a valid 10-digit Indian mobile (e.g. 9876543210)")
        String phone,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8–72 characters")
        String password
) {
}

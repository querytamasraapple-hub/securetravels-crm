package com.securetravels.crm.user.dto;

import com.securetravels.crm.user.Role;
import com.securetravels.crm.user.User;

import java.time.Instant;
import java.util.UUID;

public record TeamUserResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        String phone,
        boolean active,
        Instant createdAt
) {
    public static TeamUserResponse from(User u) {
        return new TeamUserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getRole(),
                u.getPhone(),
                u.isActive(),
                u.getCreatedAt());
    }
}

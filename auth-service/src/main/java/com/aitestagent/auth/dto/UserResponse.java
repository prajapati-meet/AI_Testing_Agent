package com.aitestagent.auth.dto;

import com.aitestagent.auth.entity.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        LocalDateTime createdAt
) {
}

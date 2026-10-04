package com.aitestagent.auth.dto;

import com.aitestagent.auth.entity.Role;

public record AuthResponse(
        String token,
        String email,
        String name,
        Role role,
        String message
) {
}

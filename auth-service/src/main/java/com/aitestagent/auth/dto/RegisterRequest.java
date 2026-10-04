package com.aitestagent.auth.dto;

import com.aitestagent.auth.entity.Role;

public record RegisterRequest(
        String name,
        String email,
        String password,
        Role role
) {
}

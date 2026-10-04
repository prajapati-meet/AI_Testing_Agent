package com.aitestagent.auth.dto;

public record LoginRequest(
        String email,
        String password
) {
}

package com.aitestagent.auth;

import com.aitestagent.auth.dto.AuthResponse;
import com.aitestagent.auth.dto.LoginRequest;
import com.aitestagent.auth.dto.RegisterRequest;
import com.aitestagent.auth.dto.UserResponse;
import com.aitestagent.auth.entity.Role;
import com.aitestagent.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthServiceApplicationTests {

    @Autowired
    private AuthService authService;

    @Test
    void contextLoadsAndRegistersUserSuccessfully() {
        RegisterRequest registerReq = new RegisterRequest("Test User", "testuser@example.com", "password123", Role.USER);
        AuthResponse authResponse = authService.register(registerReq);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.token()).isNotBlank();
        assertThat(authResponse.email()).isEqualTo("testuser@example.com");

        LoginRequest loginReq = new LoginRequest("testuser@example.com", "password123");
        AuthResponse loginResponse = authService.login(loginReq);

        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.token()).isNotBlank();
    }

    @Test
    @WithMockUser(username = "testuser@example.com")
    void contextLoadsAndReturnsCurrentUser() {
        RegisterRequest registerReq = new RegisterRequest("Current User", "testuser@example.com", "password123", Role.USER);
        try {
            authService.register(registerReq);
        } catch (IllegalArgumentException ignored) {
            // Already registered
        }

        UserResponse response = authService.getCurrentUser();
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("testuser@example.com");
    }
}

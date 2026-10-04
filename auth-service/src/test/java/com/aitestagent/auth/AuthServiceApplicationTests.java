package com.aitestagent.auth;

import com.aitestagent.auth.dto.UserResponse;
import com.aitestagent.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthServiceApplicationTests {

    @Autowired
    private AuthService authService;

    @Test
    void contextLoadsAndReturnsCurrentUserPlaceholder() {
        UserResponse response = authService.getCurrentUser();
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("user@example.com");
    }
}

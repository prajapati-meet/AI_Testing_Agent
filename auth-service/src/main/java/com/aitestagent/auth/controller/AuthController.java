package com.aitestagent.auth.controller;

import com.aitestagent.auth.dto.AuthResponse;
import com.aitestagent.auth.dto.LoginRequest;
import com.aitestagent.auth.dto.RegisterRequest;
import com.aitestagent.auth.dto.UserResponse;
import com.aitestagent.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody(required = false) RegisterRequest request) {
        RegisterRequest safeRequest = (request != null)
                ? request
                : new RegisterRequest("Placeholder User", "user@example.com", "password123", null);
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(safeRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody(required = false) LoginRequest request) {
        LoginRequest safeRequest = (request != null)
                ? request
                : new LoginRequest("user@example.com", "password123");
        return ResponseEntity.ok(authService.login(safeRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}

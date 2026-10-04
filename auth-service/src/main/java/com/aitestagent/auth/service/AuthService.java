package com.aitestagent.auth.service;

import com.aitestagent.auth.dto.AuthResponse;
import com.aitestagent.auth.dto.LoginRequest;
import com.aitestagent.auth.dto.RegisterRequest;
import com.aitestagent.auth.dto.UserResponse;
import com.aitestagent.auth.entity.Role;
import com.aitestagent.auth.entity.User;
import com.aitestagent.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        Role assignedRole = (request.role() != null) ? request.role() : Role.USER;
        String email = (request.email() != null) ? request.email() : "user@example.com";
        String name = (request.name() != null) ? request.name() : "Placeholder User";
        String rawPassword = (request.password() != null) ? request.password() : "password123";

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(assignedRole);
        user.setCreatedAt(LocalDateTime.now());

        if (!userRepository.existsByEmail(email)) {
            userRepository.save(user);
        }

        return new AuthResponse(
                "placeholder-jwt-token",
                email,
                name,
                assignedRole,
                "User registration placeholder endpoint succeeded"
        );
    }

    public AuthResponse login(LoginRequest request) {
        String email = (request.email() != null) ? request.email() : "user@example.com";
        return new AuthResponse(
                "placeholder-jwt-token",
                email,
                "Placeholder User",
                Role.USER,
                "User login placeholder endpoint succeeded"
        );
    }

    public UserResponse getCurrentUser() {
        return new UserResponse(
                1L,
                "Placeholder User",
                "user@example.com",
                Role.USER,
                LocalDateTime.now()
        );
    }
}

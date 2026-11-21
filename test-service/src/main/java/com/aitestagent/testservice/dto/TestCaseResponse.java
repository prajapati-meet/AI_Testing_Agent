package com.aitestagent.testservice.dto;

import java.time.LocalDateTime;

public record TestCaseResponse(
        Long id,
        String name,
        String description,
        String priority,
        String expectedResult,
        String steps,
        LocalDateTime createdAt
) {
    public TestCaseResponse(Long id, String name, String description, String priority, String expectedResult, LocalDateTime createdAt) {
        this(id, name, description, priority, expectedResult, null, createdAt);
    }
}

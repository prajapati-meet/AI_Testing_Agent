package com.aitestagent.testservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TestCaseResponse(
        Long id,
        String name,
        String description,
        String priority,
        String expectedResult,
        LocalDateTime createdAt,
        List<TestStepResponse> steps
) {
    public TestCaseResponse(Long id, String name, String description, String priority, String expectedResult, LocalDateTime createdAt) {
        this(id, name, description, priority, expectedResult, createdAt, List.of());
    }
}

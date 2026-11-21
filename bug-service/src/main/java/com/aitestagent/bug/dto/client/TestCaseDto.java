package com.aitestagent.bug.dto.client;

import java.time.LocalDateTime;
import java.util.List;

public record TestCaseDto(
        Long id,
        String name,
        String description,
        String priority,
        String expectedResult,
        LocalDateTime createdAt,
        List<TestStepDto> steps
) {
}

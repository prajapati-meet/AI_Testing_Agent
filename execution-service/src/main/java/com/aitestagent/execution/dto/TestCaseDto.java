package com.aitestagent.execution.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TestCaseDto(
        Long id,
        String name,
        String description,
        String priority,
        String expectedResult,
        Object steps,
        LocalDateTime createdAt
) {
}

package com.aitestagent.bug.dto;

import com.aitestagent.bug.entity.BugSeverity;

import java.time.LocalDateTime;

/**
 * DTO matching Table 4.3 specification for bug reporting.
 */
public record BugReportResponse(
        Long id,
        Long executionId,
        String title,
        BugSeverity severity,
        String description,
        String stepsToReproduce,
        String expectedResult,
        String actualResult,
        String possibleArea,
        String evidencePath,
        LocalDateTime createdAt
) {
    // Secondary constructor for backward compatibility with 9-parameter signatures
    public BugReportResponse(
            Long id,
            Long executionId,
            String title,
            BugSeverity severity,
            String description,
            String expectedResult,
            String actualResult,
            String possibleArea,
            LocalDateTime createdAt
    ) {
        this(id, executionId, title, severity, description, "", expectedResult, actualResult, possibleArea, null, createdAt);
    }

    // Alias for summary if any existing frontend/client reads summary
    public String summary() {
        return description;
    }
}

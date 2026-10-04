package com.aitestagent.bug.dto;

import com.aitestagent.bug.entity.BugSeverity;

import java.time.LocalDateTime;

public record BugReportResponse(
        Long id,
        Long executionId,
        String title,
        BugSeverity severity,
        String summary,
        String expectedResult,
        String actualResult,
        String possibleArea,
        LocalDateTime createdAt
) {
}

package com.aitestagent.execution.dto;

import com.aitestagent.execution.entity.ExecutionStatus;

import java.time.LocalDateTime;

public record ExecutionResponse(
        Long id,
        Long testCaseId,
        String targetUrl,
        ExecutionStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage,
        String screenshotPath
) {
}

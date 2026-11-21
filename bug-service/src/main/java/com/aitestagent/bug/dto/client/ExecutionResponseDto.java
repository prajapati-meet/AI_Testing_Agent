package com.aitestagent.bug.dto.client;

import java.time.LocalDateTime;

public record ExecutionResponseDto(
        Long id,
        Long testCaseId,
        String targetUrl,
        String status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage,
        String screenshotPath
) {
}

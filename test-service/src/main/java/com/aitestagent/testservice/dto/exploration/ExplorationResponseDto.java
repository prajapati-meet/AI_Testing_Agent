package com.aitestagent.testservice.dto.exploration;

import java.time.LocalDateTime;

/**
 * Mirror DTO for exploration session response from EXPLORATION-SERVICE.
 */
public record ExplorationResponseDto(
        Long id,
        String targetUrl,
        String status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String message,
        String navigationFlowSummary
) {
}

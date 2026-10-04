package com.aitestagent.exploration.dto;

import com.aitestagent.exploration.entity.ExplorationStatus;

import java.time.LocalDateTime;

public record ExplorationResponse(
        Long id,
        String targetUrl,
        ExplorationStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String message
) {
}

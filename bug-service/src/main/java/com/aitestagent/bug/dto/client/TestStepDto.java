package com.aitestagent.bug.dto.client;

public record TestStepDto(
        Long id,
        int stepOrder,
        String action,
        String target,
        String value,
        String condition
) {
}

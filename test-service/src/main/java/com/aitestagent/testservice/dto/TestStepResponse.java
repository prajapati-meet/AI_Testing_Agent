package com.aitestagent.testservice.dto;

public record TestStepResponse(
        Long id,
        int stepOrder,
        String action,
        String target,
        String value,
        String condition
) {
}

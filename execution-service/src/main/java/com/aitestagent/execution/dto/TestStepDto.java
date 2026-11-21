package com.aitestagent.execution.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TestStepDto(
        Integer stepOrder,
        String action,
        String target,
        String value
) {
}

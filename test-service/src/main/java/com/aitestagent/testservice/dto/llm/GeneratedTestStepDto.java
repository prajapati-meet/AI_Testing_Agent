package com.aitestagent.testservice.dto.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedTestStepDto(
        int stepOrder,
        String action,
        String target,
        String value,
        String condition
) {
}

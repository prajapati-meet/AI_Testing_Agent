package com.aitestagent.testservice.dto.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedTestCaseDto(
        String name,
        String description,
        String priority,
        String expectedResult,
        List<GeneratedTestStepDto> steps
) {
}

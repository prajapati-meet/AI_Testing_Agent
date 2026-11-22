package com.aitestagent.bug.dto.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeneratedBugReportDto(
        String title,
        String severity,
        String description,
        String stepsToReproduce,
        String expectedResult,
        String actualResult,
        String possibleArea,
        String suggestedFix
) {
}

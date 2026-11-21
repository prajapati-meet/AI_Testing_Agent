package com.aitestagent.execution.dto;

import java.util.List;

public record ExecutionRequest(
        Long testCaseId,
        String targetUrl,
        List<TestStepDto> steps
) {
    public ExecutionRequest(Long testCaseId, String targetUrl) {
        this(testCaseId, targetUrl, null);
    }
}

package com.aitestagent.execution.dto;

public record ExecutionRequest(
        Long testCaseId,
        String targetUrl
) {
}

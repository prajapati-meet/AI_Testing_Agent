package com.aitestagent.testservice.dto;

public record GenerateTestsRequest(
        Long explorationId,
        String targetUrl
) {
}

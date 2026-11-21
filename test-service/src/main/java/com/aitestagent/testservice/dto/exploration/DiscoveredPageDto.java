package com.aitestagent.testservice.dto.exploration;

/**
 * Mirror DTO for pages discovered by EXPLORATION-SERVICE.
 */
public record DiscoveredPageDto(
        Long id,
        Long explorationId,
        String pageUrl,
        String pageTitle,
        String discoveredLinks,
        String buttons,
        String inputs,
        String forms,
        String navigationFlow
) {
}

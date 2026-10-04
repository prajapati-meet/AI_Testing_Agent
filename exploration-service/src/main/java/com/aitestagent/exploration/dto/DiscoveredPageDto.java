package com.aitestagent.exploration.dto;

public record DiscoveredPageDto(
        String url,
        String title,
        int linksCount,
        int buttonsCount,
        int formsCount,
        int inputsCount
) {
}

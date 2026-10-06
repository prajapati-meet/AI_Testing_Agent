package com.aitestagent.exploration.dto;

/**
 * Full response DTO for a single discovered page.
 *
 * <p>The {@code buttons}, {@code inputs}, {@code forms}, and {@code discoveredLinks} fields
 * are JSON strings that can be parsed by the client or forwarded directly to the LLM.</p>
 */
public record DiscoveredPageDto(
        Long id,
        Long explorationId,
        String pageUrl,
        String pageTitle,
        /** JSON array of href strings found on this page */
        String discoveredLinks,
        /** JSON array of {text, selector} button objects */
        String buttons,
        /** JSON array of {id, name, type, placeholder} input objects */
        String inputs,
        /** JSON array of {formId, action, fields[]} form objects */
        String forms,
        /** Plain-text description of how this page connects to others */
        String navigationFlow
) {
}

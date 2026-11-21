package com.aitestagent.testservice.client;

import com.aitestagent.testservice.dto.exploration.DiscoveredPageDto;
import com.aitestagent.testservice.dto.exploration.ExplorationResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * REST client using @LoadBalanced RestTemplate to query EXPLORATION-SERVICE via Eureka.
 */
@Component
public class ExplorationClient {

    private static final Logger log = LoggerFactory.getLogger(ExplorationClient.class);
    private static final String BASE_URL = "http://EXPLORATION-SERVICE/api/exploration";

    private final RestTemplate restTemplate;

    public ExplorationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Retrieves session details including navigationFlowSummary from EXPLORATION-SERVICE.
     */
    public Optional<ExplorationResponseDto> getExploration(Long explorationId) {
        if (explorationId == null) {
            return Optional.empty();
        }
        try {
            String url = BASE_URL + "/" + explorationId;
            log.info("Querying EXPLORATION-SERVICE for exploration session: {}", url);
            ExplorationResponseDto response = restTemplate.getForObject(url, ExplorationResponseDto.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("Failed to retrieve exploration {} from EXPLORATION-SERVICE: {}", explorationId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves all discovered pages (links, buttons, inputs, forms) for an exploration session.
     */
    public List<DiscoveredPageDto> getDiscoveredPages(Long explorationId) {
        if (explorationId == null) {
            return Collections.emptyList();
        }
        try {
            String url = BASE_URL + "/" + explorationId + "/pages";
            log.info("Querying EXPLORATION-SERVICE for discovered pages: {}", url);
            ResponseEntity<List<DiscoveredPageDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            if (response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception ex) {
            log.warn("Failed to retrieve pages for exploration {} from EXPLORATION-SERVICE: {}", explorationId, ex.getMessage());
        }
        return Collections.emptyList();
    }
}

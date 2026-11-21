package com.aitestagent.bug.client;

import com.aitestagent.bug.dto.client.ExecutionResponseDto;
import com.aitestagent.bug.dto.client.TestCaseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Inter-service REST client using @LoadBalanced RestTemplate to query
 * EXECUTION-SERVICE and TEST-SERVICE via Eureka.
 */
@Component
public class ExecutionClient {

    private static final Logger log = LoggerFactory.getLogger(ExecutionClient.class);
    private static final String EXECUTION_URL = "http://EXECUTION-SERVICE/api/executions";
    private static final String TEST_URL = "http://TEST-SERVICE/api/tests";

    private final RestTemplate restTemplate;

    public ExecutionClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Retrieves execution record and failure evidence from EXECUTION-SERVICE.
     */
    public Optional<ExecutionResponseDto> getExecution(Long executionId) {
        if (executionId == null) {
            return Optional.empty();
        }
        try {
            String url = EXECUTION_URL + "/" + executionId;
            log.info("Fetching execution evidence from EXECUTION-SERVICE: {}", url);
            ExecutionResponseDto response = restTemplate.getForObject(url, ExecutionResponseDto.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("Failed to fetch execution {} from EXECUTION-SERVICE: {}", executionId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Retrieves test case details and steps from TEST-SERVICE.
     */
    public Optional<TestCaseDto> getTestCase(Long testCaseId) {
        if (testCaseId == null) {
            return Optional.empty();
        }
        try {
            String url = TEST_URL + "/" + testCaseId;
            log.info("Fetching test case details from TEST-SERVICE: {}", url);
            TestCaseDto response = restTemplate.getForObject(url, TestCaseDto.class);
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("Failed to fetch test case {} from TEST-SERVICE: {}", testCaseId, ex.getMessage());
            return Optional.empty();
        }
    }
}

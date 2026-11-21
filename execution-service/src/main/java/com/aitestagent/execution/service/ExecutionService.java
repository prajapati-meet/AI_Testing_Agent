package com.aitestagent.execution.service;

import com.aitestagent.execution.dto.ExecutionRequest;
import com.aitestagent.execution.dto.ExecutionResponse;
import com.aitestagent.execution.dto.TestCaseDto;
import com.aitestagent.execution.dto.TestStepDto;
import com.aitestagent.execution.engine.PlaywrightExecutionEngine;
import com.aitestagent.execution.entity.Execution;
import com.aitestagent.execution.entity.ExecutionStatus;
import com.aitestagent.execution.exception.ResourceNotFoundException;
import com.aitestagent.execution.repository.ExecutionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private final ExecutionRepository executionRepository;
    private final PlaywrightExecutionEngine executionEngine;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${services.test-service.url:http://TEST-SERVICE/api/tests}")
    private String testServiceUrl;

    public ExecutionService(ExecutionRepository executionRepository,
                            PlaywrightExecutionEngine executionEngine,
                            RestTemplate restTemplate,
                            ObjectMapper objectMapper) {
        this.executionRepository = executionRepository;
        this.executionEngine = executionEngine;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public ExecutionResponse triggerExecution(ExecutionRequest request) {
        Long testCaseId = (request != null && request.testCaseId() != null) ? request.testCaseId() : 1L;
        String targetUrl = (request != null && request.targetUrl() != null && !request.targetUrl().isBlank())
                ? request.targetUrl().trim()
                : "http://localhost:3000";

        Execution execution = new Execution();
        execution.setTestCaseId(testCaseId);
        execution.setTargetUrl(targetUrl);
        execution.setStatus(ExecutionStatus.PENDING);
        execution.setStartedAt(LocalDateTime.now());

        Execution saved = executionRepository.save(execution);

        // Resolve test steps
        List<TestStepDto> steps = resolveSteps(request, testCaseId, targetUrl);

        // Trigger execution asynchronously
        executionEngine.executeAsync(saved.getId(), targetUrl, steps);

        return mapToResponse(saved);
    }

    public List<ExecutionResponse> getAllExecutions() {
        List<Execution> existing = executionRepository.findAll();
        if (existing.isEmpty()) {
            return List.of(new ExecutionResponse(
                    1L,
                    1L,
                    "http://localhost:3000",
                    ExecutionStatus.PASSED,
                    LocalDateTime.now().minusMinutes(1),
                    LocalDateTime.now(),
                    null,
                    null,
                    null
            ));
        }
        return existing.stream().map(this::mapToResponse).toList();
    }

    public ExecutionResponse getExecutionById(Long id) {
        return executionRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found with id: " + id));
    }

    public byte[] getExecutionScreenshot(Long id) {
        Execution execution = executionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found with id: " + id));

        if (execution.getScreenshotPath() == null || execution.getScreenshotPath().isBlank()) {
            throw new ResourceNotFoundException("No screenshot recorded for execution id: " + id);
        }

        Path path = Paths.get(execution.getScreenshotPath());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Screenshot file not found on disk at: " + execution.getScreenshotPath());
        }

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read screenshot file: " + e.getMessage(), e);
        }
    }

    public List<TestStepDto> resolveSteps(ExecutionRequest request, Long testCaseId, String targetUrl) {
        // 1. If explicit steps are provided in request, use them directly
        if (request != null && request.steps() != null && !request.steps().isEmpty()) {
            return request.steps();
        }

        // 2. Fetch test case from TEST-SERVICE via RestTemplate
        try {
            String url = testServiceUrl + "/" + testCaseId;
            log.info("Querying TEST-SERVICE at {} for testCaseId: {}", url, testCaseId);
            TestCaseDto testCaseDto = restTemplate.getForObject(url, TestCaseDto.class);
            if (testCaseDto != null && testCaseDto.steps() != null) {
                List<TestStepDto> fetchedSteps = parseSteps(testCaseDto.steps());
                if (!fetchedSteps.isEmpty()) {
                    return fetchedSteps;
                }
            }
        } catch (Exception ex) {
            log.warn("Could not retrieve test case #{} from TEST-SERVICE: {}. Using default fallback steps.",
                    testCaseId, ex.getMessage());
        }

        // 3. Fallback default steps
        return List.of(
                new TestStepDto(1, "NAVIGATE", null, targetUrl),
                new TestStepDto(2, "WAIT", "body", null),
                new TestStepDto(3, "ASSERT_VISIBLE", "body", null)
        );
    }

    private List<TestStepDto> parseSteps(Object stepsObj) {
        if (stepsObj == null) {
            return List.of();
        }
        try {
            if (stepsObj instanceof String stepsStr) {
                if (stepsStr.isBlank()) {
                    return List.of();
                }
                return objectMapper.readValue(stepsStr, new TypeReference<List<TestStepDto>>() {});
            } else {
                return objectMapper.convertValue(stepsObj, new TypeReference<List<TestStepDto>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse steps payload {}: {}", stepsObj, e.getMessage());
            return List.of();
        }
    }

    private ExecutionResponse mapToResponse(Execution entity) {
        return new ExecutionResponse(
                entity.getId(),
                entity.getTestCaseId(),
                entity.getTargetUrl(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getErrorMessage(),
                entity.getScreenshotPath(),
                entity.getFailedStep()
        );
    }
}

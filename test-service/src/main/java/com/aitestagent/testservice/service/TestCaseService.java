package com.aitestagent.testservice.service;

import com.aitestagent.testservice.client.ExplorationClient;
import com.aitestagent.testservice.dto.GenerateTestsRequest;
import com.aitestagent.testservice.dto.TestCaseResponse;
import com.aitestagent.testservice.dto.TestStepResponse;
import com.aitestagent.testservice.dto.exploration.DiscoveredPageDto;
import com.aitestagent.testservice.dto.exploration.ExplorationResponseDto;
import com.aitestagent.testservice.entity.TestCase;
import com.aitestagent.testservice.entity.TestStep;
import com.aitestagent.testservice.exception.ResourceNotFoundException;
import com.aitestagent.testservice.repository.TestCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class TestCaseService {

    private static final Logger log = LoggerFactory.getLogger(TestCaseService.class);

    private final TestCaseRepository testCaseRepository;
    private final ExplorationClient explorationClient;
    private final LlmTestGenerationService llmTestGenerationService;

    public TestCaseService(
            TestCaseRepository testCaseRepository,
            ExplorationClient explorationClient,
            LlmTestGenerationService llmTestGenerationService
    ) {
        this.testCaseRepository = testCaseRepository;
        this.explorationClient = explorationClient;
        this.llmTestGenerationService = llmTestGenerationService;
    }

    @Transactional
    public List<TestCaseResponse> generateTestCases(GenerateTestsRequest request) {
        return generateTestCases(request, null);
    }

    @Transactional
    public List<TestCaseResponse> generateTestCases(GenerateTestsRequest request, String userEmail) {
        Long explorationId = request != null ? request.explorationId() : null;
        String targetUrl = request != null ? request.targetUrl() : null;

        ExplorationResponseDto exploration = null;
        List<DiscoveredPageDto> pages = Collections.emptyList();

        if (explorationId != null) {
            log.info("Fetching exploration metadata and pages for session id: {}", explorationId);
            exploration = explorationClient.getExploration(explorationId).orElse(null);
            pages = explorationClient.getDiscoveredPages(explorationId);

            if ((targetUrl == null || targetUrl.isBlank()) && exploration != null && exploration.targetUrl() != null) {
                targetUrl = exploration.targetUrl();
            }
        }

        if (targetUrl == null || targetUrl.isBlank()) {
            targetUrl = "http://localhost:3000";
        }

        log.info("Triggering AI test generation for target: {}, explorationId: {}, discovered pages: {}, user: {}",
                targetUrl, explorationId, pages.size(), userEmail);

        List<TestCase> generatedCases = llmTestGenerationService.generateTestCases(
                targetUrl, explorationId, exploration, pages
        );

        if (userEmail != null && !userEmail.isBlank()) {
            for (TestCase tc : generatedCases) {
                tc.setUserEmail(userEmail);
            }
        }

        List<TestCase> saved = testCaseRepository.saveAll(generatedCases);
        log.info("Persisted {} test cases with steps to test_db for user {}.", saved.size(), userEmail);

        return saved.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TestCaseResponse> getAllTestCases() {
        return getAllTestCases(null);
    }

    @Transactional(readOnly = true)
    public List<TestCaseResponse> getAllTestCases(String userEmail) {
        List<TestCase> list;
        if (userEmail != null && !userEmail.isBlank()) {
            list = testCaseRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
        } else {
            list = testCaseRepository.findAllByOrderByCreatedAtDesc();
        }
        return list.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public TestCaseResponse getTestCaseById(Long id) {
        return testCaseRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + id));
    }

    public TestCaseResponse mapToResponse(TestCase entity) {
        List<TestStepResponse> stepResponses = (entity.getSteps() != null)
                ? entity.getSteps().stream()
                        .sorted(Comparator.comparingInt(TestStep::getStepOrder))
                        .map(s -> new TestStepResponse(
                                s.getId(),
                                s.getStepOrder(),
                                s.getAction() != null ? s.getAction().name() : "WAIT",
                                s.getTarget(),
                                s.getValue(),
                                s.getCondition()
                        ))
                        .toList()
                : List.of();

        return new TestCaseResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPriority(),
                entity.getExpectedResult(),
                entity.getCreatedAt(),
                stepResponses
        );
    }
}

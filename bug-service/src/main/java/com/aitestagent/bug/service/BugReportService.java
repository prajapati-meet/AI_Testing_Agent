package com.aitestagent.bug.service;

import com.aitestagent.bug.client.ExecutionClient;
import com.aitestagent.bug.dto.BugReportResponse;
import com.aitestagent.bug.dto.client.ExecutionResponseDto;
import com.aitestagent.bug.dto.client.TestCaseDto;
import com.aitestagent.bug.entity.BugReport;
import com.aitestagent.bug.entity.BugSeverity;
import com.aitestagent.bug.exception.ResourceNotFoundException;
import com.aitestagent.bug.repository.BugReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BugReportService {

    private static final Logger log = LoggerFactory.getLogger(BugReportService.class);

    private final BugReportRepository bugReportRepository;
    private final ExecutionClient executionClient;
    private final LlmBugAnalysisService llmBugAnalysisService;

    public BugReportService(
            BugReportRepository bugReportRepository,
            ExecutionClient executionClient,
            LlmBugAnalysisService llmBugAnalysisService
    ) {
        this.bugReportRepository = bugReportRepository;
        this.executionClient = executionClient;
        this.llmBugAnalysisService = llmBugAnalysisService;
    }

    @Transactional
    public BugReportResponse analyzeExecution(Long executionId) {
        return analyzeExecution(executionId, null);
    }

    @Transactional
    public BugReportResponse analyzeExecution(Long executionId, String userEmail) {
        log.info("Starting AI bug analysis for execution #{}, user: {}", executionId, userEmail);

        ExecutionResponseDto execution = executionClient.getExecution(executionId).orElse(null);
        TestCaseDto testCase = null;
        if (execution != null && execution.testCaseId() != null) {
            testCase = executionClient.getTestCase(execution.testCaseId()).orElse(null);
        }

        BugReport bugReport = llmBugAnalysisService.analyzeFailure(executionId, execution, testCase);
        if (userEmail != null && !userEmail.isBlank()) {
            bugReport.setUserEmail(userEmail);
        }
        BugReport saved = bugReportRepository.save(bugReport);
        log.info("Persisted AI bug report #{} for execution #{} with severity {}",
                saved.getId(), executionId, saved.getSeverity());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BugReportResponse> getAllBugReports() {
        return getAllBugReports(null);
    }

    @Transactional(readOnly = true)
    public List<BugReportResponse> getAllBugReports(String userEmail) {
        List<BugReport> existing;
        if (userEmail != null && !userEmail.isBlank()) {
            existing = bugReportRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
        } else {
            existing = bugReportRepository.findAllByOrderByCreatedAtDesc();
        }
        return existing.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public BugReportResponse getBugReportById(Long id) {
        return bugReportRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Bug report not found with id: " + id));
    }

    public BugReportResponse mapToResponse(BugReport entity) {
        return new BugReportResponse(
                entity.getId(),
                entity.getExecutionId(),
                entity.getTitle(),
                entity.getSeverity(),
                entity.getDescription(),
                entity.getStepsToReproduce(),
                entity.getExpectedResult(),
                entity.getActualResult(),
                entity.getPossibleArea(),
                entity.getEvidencePath(),
                entity.getCreatedAt(),
                entity.getSuggestedFix()
        );
    }
}

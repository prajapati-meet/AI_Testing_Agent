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
        log.info("Starting AI bug analysis for execution #{}", executionId);

        ExecutionResponseDto execution = executionClient.getExecution(executionId).orElse(null);
        TestCaseDto testCase = null;
        if (execution != null && execution.testCaseId() != null) {
            testCase = executionClient.getTestCase(execution.testCaseId()).orElse(null);
        }

        BugReport bugReport = llmBugAnalysisService.analyzeFailure(executionId, execution, testCase);
        BugReport saved = bugReportRepository.save(bugReport);
        log.info("Persisted AI bug report #{} for execution #{} with severity {}",
                saved.getId(), executionId, saved.getSeverity());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BugReportResponse> getAllBugReports() {
        List<BugReport> existing = bugReportRepository.findAll();
        if (existing.isEmpty()) {
            log.info("No bug reports found in bug_db. Providing fallback initial analysis.");
            return List.of(new BugReportResponse(
                    1L,
                    1L,
                    "Authentication Failure: Valid Credentials Rejected Under BUG_MODE",
                    BugSeverity.HIGH,
                    "Automated test execution failed during authentication workflow against demo-target-app. Although valid demo credentials were provided, the authentication form returned an unexpected error banner.",
                    "1. Navigate to http://localhost:3000/login\n2. Enter valid email (test@example.com)\n3. Enter valid password (password123)\n4. Click Login\n5. Observe error banner displayed",
                    "User should be authenticated and redirected to /dashboard.",
                    "Login failed: Authentication service returned an unexpected error (BUG_MODE=true).",
                    "Authentication Service / Login Component (demo-target-app / auth-service)",
                    "screenshots/exec-1.png",
                    LocalDateTime.now()
            ));
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
                entity.getCreatedAt()
        );
    }
}

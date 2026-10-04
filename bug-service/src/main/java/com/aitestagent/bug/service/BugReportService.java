package com.aitestagent.bug.service;

import com.aitestagent.bug.dto.BugReportResponse;
import com.aitestagent.bug.entity.BugReport;
import com.aitestagent.bug.entity.BugSeverity;
import com.aitestagent.bug.repository.BugReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BugReportService {

    private final BugReportRepository bugReportRepository;

    public BugReportService(BugReportRepository bugReportRepository) {
        this.bugReportRepository = bugReportRepository;
    }

    public BugReportResponse analyzeExecution(Long executionId) {
        BugReport bugReport = new BugReport();
        bugReport.setExecutionId(executionId);
        bugReport.setTitle("Login Failure Under BUG_MODE (Placeholder)");
        bugReport.setSeverity(BugSeverity.HIGH);
        bugReport.setSummary("Placeholder AI bug analysis for failed execution #" + executionId);
        bugReport.setExpectedResult("User with valid credentials should be redirected to /dashboard");
        bugReport.setActualResult("Login form returned error despite valid demo credentials");
        bugReport.setPossibleArea("Authentication / Login Workflow");
        bugReport.setCreatedAt(LocalDateTime.now());

        BugReport saved = bugReportRepository.save(bugReport);
        return mapToResponse(saved);
    }

    public List<BugReportResponse> getAllBugReports() {
        List<BugReport> existing = bugReportRepository.findAll();
        if (existing.isEmpty()) {
            return List.of(new BugReportResponse(
                    1L,
                    1L,
                    "Login Failure Under BUG_MODE (Placeholder)",
                    BugSeverity.HIGH,
                    "Placeholder bug report summary",
                    "User logs in and reaches /dashboard",
                    "Login rejected valid credentials",
                    "Authentication / Login Workflow",
                    LocalDateTime.now()
            ));
        }
        return existing.stream().map(this::mapToResponse).toList();
    }

    public BugReportResponse getBugReportById(Long id) {
        return bugReportRepository.findById(id)
                .map(this::mapToResponse)
                .orElseGet(() -> new BugReportResponse(
                        id,
                        1L,
                        "Login Failure Under BUG_MODE (Placeholder)",
                        BugSeverity.HIGH,
                        "Placeholder bug report summary",
                        "User logs in and reaches /dashboard",
                        "Login rejected valid credentials",
                        "Authentication / Login Workflow",
                        LocalDateTime.now()
                ));
    }

    private BugReportResponse mapToResponse(BugReport entity) {
        return new BugReportResponse(
                entity.getId(),
                entity.getExecutionId(),
                entity.getTitle(),
                entity.getSeverity(),
                entity.getSummary(),
                entity.getExpectedResult(),
                entity.getActualResult(),
                entity.getPossibleArea(),
                entity.getCreatedAt()
        );
    }
}

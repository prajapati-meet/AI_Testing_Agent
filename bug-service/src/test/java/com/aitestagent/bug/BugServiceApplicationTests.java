package com.aitestagent.bug;

import com.aitestagent.bug.dto.BugReportResponse;
import com.aitestagent.bug.dto.client.ExecutionResponseDto;
import com.aitestagent.bug.dto.client.TestCaseDto;
import com.aitestagent.bug.dto.client.TestStepDto;
import com.aitestagent.bug.entity.BugReport;
import com.aitestagent.bug.entity.BugSeverity;
import com.aitestagent.bug.service.BugReportService;
import com.aitestagent.bug.service.LlmBugAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BugServiceApplicationTests {

    @Autowired
    private BugReportService bugReportService;

    @Autowired
    private LlmBugAnalysisService llmBugAnalysisService;

    @Test
    @DisplayName("Context loads and getAllBugReports returns structured reports with Table 4.3 fields")
    void contextLoadsAndReturnsBugReports() {
        List<BugReportResponse> bugReports = bugReportService.getAllBugReports();
        assertThat(bugReports).isNotEmpty();

        BugReportResponse first = bugReports.get(0);
        assertThat(first.title()).isNotBlank();
        assertThat(first.severity()).isNotNull();
        assertThat(first.description()).isNotBlank();
        assertThat(first.stepsToReproduce()).isNotBlank();
        assertThat(first.expectedResult()).isNotBlank();
        assertThat(first.actualResult()).isNotBlank();
        assertThat(first.possibleArea()).isNotBlank();
    }

    @Test
    @DisplayName("analyzeExecution synthesizes and persists bug report when LLM key is absent")
    void testAnalyzeExecutionFallback() {
        BugReportResponse analyzed = bugReportService.analyzeExecution(101L);

        assertThat(analyzed).isNotNull();
        assertThat(analyzed.id()).isNotNull();
        assertThat(analyzed.executionId()).isEqualTo(101L);
        assertThat(analyzed.title()).isNotBlank();
        assertThat(analyzed.severity()).isNotNull();
        assertThat(analyzed.description()).isNotBlank();
        assertThat(analyzed.stepsToReproduce()).contains("1.");
        assertThat(analyzed.expectedResult()).isNotBlank();
        assertThat(analyzed.actualResult()).isNotBlank();

        // Verify retrieval by ID
        BugReportResponse retrieved = bugReportService.getBugReportById(analyzed.id());
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo(analyzed.id());
        assertThat(retrieved.title()).isEqualTo(analyzed.title());
    }

    @Test
    @DisplayName("LlmBugAnalysisService parses markdown-wrapped JSON and validates schema")
    void testParseAndValidateLlmResponseWithMarkdown() {
        String mockLlmOutput = """
                ```json
                {
                  "title": "Checkout Button Disabled Due to Empty Cart State",
                  "severity": "CRITICAL",
                  "description": "Cart state was reset upon navigation causing checkout to be disabled.",
                  "stepsToReproduce": "1. Navigate to /cart\\n2. Click Checkout\\n3. Observe button disabled",
                  "expectedResult": "User should proceed to payment view",
                  "actualResult": "Checkout button remains disabled with opacity 0.5",
                  "possibleArea": "Cart Checkout Component (/src/pages/Cart.jsx)"
                }
                ```
                """;

        ExecutionResponseDto execution = new ExecutionResponseDto(
                202L,
                15L,
                "http://localhost:3000/cart",
                "FAILED",
                LocalDateTime.now().minusMinutes(2),
                LocalDateTime.now(),
                "Checkout button is not clickable",
                "screenshots/exec-202.png"
        );

        BugReport report = llmBugAnalysisService.parseAndValidateLlmResponse(mockLlmOutput, 202L, execution);

        assertThat(report).isNotNull();
        assertThat(report.getTitle()).isEqualTo("Checkout Button Disabled Due to Empty Cart State");
        assertThat(report.getSeverity()).isEqualTo(BugSeverity.CRITICAL);
        assertThat(report.getStepsToReproduce()).contains("1. Navigate to /cart");
        assertThat(report.getEvidencePath()).isEqualTo("screenshots/exec-202.png");
    }

    @Test
    @DisplayName("Deterministic fallback identifies BUG_MODE authentication defect from test steps")
    void testDeterministicBugReportAuthFailure() {
        ExecutionResponseDto execution = new ExecutionResponseDto(
                303L,
                20L,
                "http://localhost:3000/login",
                "FAILED",
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now(),
                "Login failed: Authentication service returned an unexpected error (BUG_MODE=true).",
                "screenshots/exec-303.png"
        );

        TestCaseDto testCase = new TestCaseDto(
                20L,
                "Verify Login Workflow",
                "Valid credentials authentication test",
                "HIGH",
                "User redirected to /dashboard",
                LocalDateTime.now(),
                List.of(
                        new TestStepDto(1L, 1, "NAVIGATE", "http://localhost:3000/login", "", ""),
                        new TestStepDto(2L, 2, "FILL", "#email", "test@example.com", ""),
                        new TestStepDto(3L, 3, "FILL", "#password", "password123", ""),
                        new TestStepDto(4L, 4, "CLICK", "button#login-submit", "", "")
                )
        );

        BugReport report = llmBugAnalysisService.generateDeterministicBugReport(303L, execution, testCase);

        assertThat(report).isNotNull();
        assertThat(report.getTitle()).contains("BUG_MODE");
        assertThat(report.getSeverity()).isEqualTo(BugSeverity.HIGH);
        assertThat(report.getStepsToReproduce()).contains("FILL #email with value 'test@example.com'");
        assertThat(report.getActualResult()).contains("BUG_MODE=true");
        assertThat(report.getPossibleArea()).contains("Authentication");
    }
}

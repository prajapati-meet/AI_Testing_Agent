package com.aitestagent.execution.engine;

import com.aitestagent.execution.dto.TestStepDto;
import com.aitestagent.execution.entity.Execution;
import com.aitestagent.execution.entity.ExecutionStatus;
import com.aitestagent.execution.repository.ExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PlaywrightExecutionEngineTest {

    @Autowired
    private PlaywrightExecutionEngine executionEngine;

    @Autowired
    private ExecutionRepository executionRepository;

    private Execution savedExecution;

    @BeforeEach
    void setUp() {
        Execution execution = new Execution();
        execution.setTestCaseId(101L);
        execution.setTargetUrl("http://localhost:3000");
        execution.setStatus(ExecutionStatus.PENDING);
        execution.setStartedAt(LocalDateTime.now());
        savedExecution = executionRepository.save(execution);
    }

    @Test
    @DisplayName("Should execute all steps successfully and mark status as PASSED")
    void shouldExecuteAllStepsSuccessfully() {
        String testHtml = """
                data:text/html,<!DOCTYPE html>
                <html>
                <body>
                  <h1 id='header'>Dashboard</h1>
                  <input id='search-input' type='text' />
                  <button id='search-btn' onclick='document.getElementById("header").innerText="Results"'>Search</button>
                </body>
                </html>
                """.replace("\n", "").trim();

        List<TestStepDto> steps = List.of(
                new TestStepDto(1, "NAVIGATE", null, testHtml),
                new TestStepDto(2, "WAIT", "#header", null),
                new TestStepDto(3, "ASSERT_VISIBLE", "#header", null),
                new TestStepDto(4, "ASSERT_TEXT", "#header", "Dashboard"),
                new TestStepDto(5, "FILL", "#search-input", "Playwright Automation"),
                new TestStepDto(6, "CLICK", "#search-btn", null),
                new TestStepDto(7, "ASSERT_TEXT", "#header", "Results")
        );

        Execution result = executionEngine.execute(savedExecution.getId(), "http://localhost:3000", steps);

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.PASSED);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getFailedStep()).isNull();
    }

    @Test
    @DisplayName("Should classify assertion failure as FAILED and capture screenshot evidence")
    void shouldFailOnAssertionErrorAndCaptureScreenshot() {
        String testHtml = """
                data:text/html,<!DOCTYPE html>
                <html>
                <body>
                  <h1 id='welcome-msg'>Welcome Guest</h1>
                </body>
                </html>
                """.replace("\n", "").trim();

        List<TestStepDto> steps = List.of(
                new TestStepDto(1, "NAVIGATE", null, testHtml),
                new TestStepDto(2, "WAIT", "#welcome-msg", null),
                // Step 3 will fail because expected text does not match
                new TestStepDto(3, "ASSERT_TEXT", "#welcome-msg", "Welcome Admin")
        );

        Execution result = executionEngine.execute(savedExecution.getId(), "http://localhost:3000", steps);

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(result.getFailedStep()).isEqualTo(3);
        assertThat(result.getErrorMessage()).contains("Step 3 failed");
        assertThat(result.getScreenshotPath()).isNotNull();

        // Verify screenshot file actually exists on disk
        Path screenshotFile = Paths.get(result.getScreenshotPath());
        assertThat(Files.exists(screenshotFile)).isTrue();
        assertThat(screenshotFile.toFile().length()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should classify missing element as FAILED and record failed step")
    void shouldFailOnMissingElement() {
        String testHtml = "data:text/html,<html><body><div>Empty</div></body></html>";

        List<TestStepDto> steps = List.of(
                new TestStepDto(1, "NAVIGATE", null, testHtml),
                new TestStepDto(2, "CLICK", "#non-existent-button", null)
        );

        Execution result = executionEngine.execute(savedExecution.getId(), "http://localhost:3000", steps);

        assertThat(result.getStatus()).isIn(ExecutionStatus.FAILED, ExecutionStatus.TIMEOUT);
        assertThat(result.getFailedStep()).isEqualTo(2);
        assertThat(result.getErrorMessage()).isNotNull();
        assertThat(result.getScreenshotPath()).isNotNull();
    }
}

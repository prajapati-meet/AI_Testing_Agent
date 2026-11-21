package com.aitestagent.execution.engine;

import com.aitestagent.execution.dto.TestStepDto;
import com.aitestagent.execution.entity.Execution;
import com.aitestagent.execution.entity.ExecutionStatus;
import com.aitestagent.execution.repository.ExecutionRepository;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class PlaywrightExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightExecutionEngine.class);

    private final ExecutionRepository executionRepository;

    @Value("${playwright.headless:true}")
    private boolean headless;

    @Value("${playwright.screenshots-dir:screenshots}")
    private String screenshotsDir;

    public PlaywrightExecutionEngine(ExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    public Execution execute(Long executionId, String defaultTargetUrl, List<TestStepDto> steps) {
        Execution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution record not found with id: " + executionId));

        execution.setStatus(ExecutionStatus.RUNNING);
        executionRepository.save(execution);

        Integer currentStepNumber = null;
        Playwright playwright = null;
        Browser browser = null;
        Page page = null;

        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(headless)
            );
            page = browser.newPage();

            if (steps != null && !steps.isEmpty()) {
                for (int i = 0; i < steps.size(); i++) {
                    TestStepDto step = steps.get(i);
                    currentStepNumber = (step.stepOrder() != null) ? step.stepOrder() : (i + 1);
                    executeStep(page, step, defaultTargetUrl);
                }
            } else if (defaultTargetUrl != null && !defaultTargetUrl.isBlank()) {
                currentStepNumber = 1;
                page.navigate(defaultTargetUrl);
            }

            execution.setStatus(ExecutionStatus.PASSED);
            execution.setCompletedAt(LocalDateTime.now());
            execution.setErrorMessage(null);
            execution.setFailedStep(null);
            log.info("Execution #{} passed all steps successfully", executionId);

        } catch (Throwable ex) {
            log.error("Execution #{} failed at step {}", executionId, currentStepNumber, ex);
            handleFailure(execution, page, currentStepNumber, ex);
        } finally {
            closePlaywrightResources(page, browser, playwright);
            execution = executionRepository.save(execution);
        }

        return execution;
    }

    @Async("executionTaskExecutor")
    public CompletableFuture<Execution> executeAsync(Long executionId, String defaultTargetUrl, List<TestStepDto> steps) {
        Execution result = execute(executionId, defaultTargetUrl, steps);
        return CompletableFuture.completedFuture(result);
    }

    private void executeStep(Page page, TestStepDto step, String defaultTargetUrl) {
        String action = (step.action() != null) ? step.action().trim().toUpperCase() : "";
        log.info("Executing step [order={}, action={}, target={}, value={}]",
                step.stepOrder(), action, step.target(), step.value());

        switch (action) {
            case "NAVIGATE" -> {
                String url = (step.value() != null && !step.value().isBlank())
                        ? step.value().trim()
                        : ((step.target() != null && !step.target().isBlank()) ? step.target().trim() : defaultTargetUrl);
                page.navigate(url);
            }
            case "FILL" -> {
                if (step.target() == null || step.target().isBlank()) {
                    throw new AssertionError("Target element selector is required for FILL action");
                }
                page.waitForSelector(step.target().trim(), new Page.WaitForSelectorOptions().setTimeout(5000));
                page.fill(step.target().trim(), step.value() != null ? step.value() : "");
            }
            case "CLICK" -> {
                if (step.target() == null || step.target().isBlank()) {
                    throw new AssertionError("Target element selector is required for CLICK action");
                }
                page.waitForSelector(step.target().trim(), new Page.WaitForSelectorOptions().setTimeout(5000));
                page.click(step.target().trim());
            }
            case "WAIT" -> {
                if (step.target() == null || step.target().isBlank()) {
                    throw new AssertionError("Target element selector is required for WAIT action");
                }
                page.waitForSelector(step.target().trim(), new Page.WaitForSelectorOptions().setTimeout(5000));
            }
            case "ASSERT_VISIBLE" -> {
                if (step.target() == null || step.target().isBlank()) {
                    throw new AssertionError("Target element selector is required for ASSERT_VISIBLE action");
                }
                page.waitForSelector(step.target().trim(), new Page.WaitForSelectorOptions().setTimeout(5000));
                if (!page.isVisible(step.target().trim())) {
                    throw new AssertionError("Element is not visible: " + step.target());
                }
            }
            case "ASSERT_TEXT" -> {
                if (step.target() == null || step.target().isBlank()) {
                    throw new AssertionError("Target element selector is required for ASSERT_TEXT action");
                }
                page.waitForSelector(step.target().trim(), new Page.WaitForSelectorOptions().setTimeout(5000));
                String innerText = page.innerText(step.target().trim());
                String expected = step.value() != null ? step.value() : "";
                if (innerText == null || !innerText.contains(expected)) {
                    throw new AssertionError("Expected innerText to contain '" + expected
                            + "', but actual innerText was '" + innerText + "' for selector: " + step.target());
                }
            }
            default -> throw new IllegalArgumentException("Unsupported test step action: " + action);
        }
    }

    private void handleFailure(Execution execution, Page page, Integer failedStepNumber, Throwable ex) {
        execution.setFailedStep(failedStepNumber);
        execution.setCompletedAt(LocalDateTime.now());

        String rawError = ex.getMessage() != null ? ex.getMessage() : ex.toString();
        String formattedError = (failedStepNumber != null ? "Step " + failedStepNumber + " failed: " : "") + rawError;
        if (formattedError.length() > 990) {
            formattedError = formattedError.substring(0, 990) + "...";
        }
        execution.setErrorMessage(formattedError);

        // Result classification:
        // - Timeout -> TIMEOUT
        // - Assertion error or element not found -> FAILED
        // - Unexpected crash -> ERROR
        if (ex instanceof TimeoutError || isTimeoutException(ex)) {
            execution.setStatus(ExecutionStatus.TIMEOUT);
        } else if (ex instanceof AssertionError || isElementNotFound(ex)) {
            execution.setStatus(ExecutionStatus.FAILED);
        } else {
            execution.setStatus(ExecutionStatus.ERROR);
        }

        // Capture screenshot evidence on failure or timeout
        captureScreenshot(execution, page);
    }

    private void captureScreenshot(Execution execution, Page page) {
        if (page == null || page.isClosed()) {
            return;
        }
        try {
            Path directory = Paths.get(screenshotsDir);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
            Path screenshotPath = directory.resolve("exec-" + execution.getId() + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(screenshotPath));
            String normalizedPath = screenshotPath.toString().replace('\\', '/');
            execution.setScreenshotPath(normalizedPath);
            log.info("Captured screenshot for execution #{} at {}", execution.getId(), normalizedPath);
        } catch (Exception e) {
            log.warn("Failed to capture screenshot for execution #{}: {}", execution.getId(), e.getMessage());
        }
    }

    private boolean isTimeoutException(Throwable t) {
        while (t != null) {
            if (t instanceof TimeoutError || t instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            String msg = t.getMessage();
            if (msg != null && (msg.contains("Timeout") || msg.contains("timeout") || msg.contains("timed out"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private boolean isElementNotFound(Throwable t) {
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && (msg.contains("Element not found")
                    || msg.contains("not visible")
                    || msg.contains("waiting for selector")
                    || msg.contains("Target element selector is required"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private void closePlaywrightResources(Page page, Browser browser, Playwright playwright) {
        try {
            if (page != null) page.close();
        } catch (Exception e) {
            log.debug("Error closing page: {}", e.getMessage());
        }
        try {
            if (browser != null) browser.close();
        } catch (Exception e) {
            log.debug("Error closing browser: {}", e.getMessage());
        }
        try {
            if (playwright != null) playwright.close();
        } catch (Exception e) {
            log.debug("Error closing playwright: {}", e.getMessage());
        }
    }
}

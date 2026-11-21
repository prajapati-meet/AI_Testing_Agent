package com.aitestagent.bug.service;

import com.aitestagent.bug.dto.client.ExecutionResponseDto;
import com.aitestagent.bug.dto.client.TestCaseDto;
import com.aitestagent.bug.dto.client.TestStepDto;
import com.aitestagent.bug.dto.llm.GeneratedBugReportDto;
import com.aitestagent.bug.entity.BugReport;
import com.aitestagent.bug.entity.BugSeverity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI-powered failure analysis service with LLM reasoning and deterministic fallback engine.
 */
@Service
public class LlmBugAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(LlmBugAnalysisService.class);

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:gemini-1.5-flash}")
    private String model;

    @Value("${llm.endpoint:}")
    private String endpoint;

    public LlmBugAnalysisService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    /**
     * Analyzes execution failure evidence and generates a structured BugReport.
     */
    public BugReport analyzeFailure(Long executionId, ExecutionResponseDto execution, TestCaseDto testCase) {
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                log.info("Analyzing failure with LLM (model: {}, endpoint: {}) for execution #{}", model, endpoint, executionId);
                BugReport report = callLlm(executionId, execution, testCase);
                if (report != null && report.getTitle() != null && !report.getTitle().isBlank()) {
                    log.info("Successfully generated AI bug report for execution #{}", executionId);
                    return report;
                }
                log.warn("LLM returned empty bug analysis. Falling back to deterministic synthesis.");
            } catch (Exception ex) {
                log.warn("Failed to generate bug report via LLM for execution #{}: {}. Falling back to deterministic synthesis.",
                        executionId, ex.getMessage());
            }
        } else {
            log.info("No LLM API key configured (LLM_API_KEY). Utilizing intelligent deterministic bug synthesizer.");
        }

        return generateDeterministicBugReport(executionId, execution, testCase);
    }

    // -------------------------------------------------------------------------
    // LLM Calling Logic
    // -------------------------------------------------------------------------

    private BugReport callLlm(Long executionId, ExecutionResponseDto execution, TestCaseDto testCase) throws Exception {
        String prompt = buildPrompt(executionId, execution, testCase);
        String rawResponse = executeLlmRequest(prompt);
        return parseAndValidateLlmResponse(rawResponse, executionId, execution);
    }

    private String buildPrompt(Long executionId, ExecutionResponseDto execution, TestCaseDto testCase) {
        String targetUrl = (execution != null && execution.targetUrl() != null) ? execution.targetUrl() : "http://localhost:3000";
        String errorMessage = (execution != null && execution.errorMessage() != null) ? execution.errorMessage() : "Assertion error or unexpected element state.";
        String screenshotPath = (execution != null && execution.screenshotPath() != null) ? execution.screenshotPath() : "N/A";

        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert Senior QA Automation Architect and Lead Site Reliability Engineer.\n");
        sb.append("Analyze the following automated Playwright end-to-end test execution failure:\n\n");
        sb.append("Execution ID: ").append(executionId).append("\n");
        sb.append("Target URL: ").append(targetUrl).append("\n");
        sb.append("Error Message / Assertion Failure: ").append(errorMessage).append("\n");
        sb.append("Failure Screenshot: ").append(screenshotPath).append("\n");

        if (testCase != null) {
            sb.append("\nFailed Test Case: ").append(testCase.name()).append("\n");
            if (testCase.description() != null) {
                sb.append("Description: ").append(testCase.description()).append("\n");
            }
            if (testCase.expectedResult() != null) {
                sb.append("Expected Outcome: ").append(testCase.expectedResult()).append("\n");
            }
            if (testCase.steps() != null && !testCase.steps().isEmpty()) {
                sb.append("\nExecuted Test Steps:\n");
                for (TestStepDto step : testCase.steps()) {
                    sb.append(step.stepOrder()).append(". ")
                            .append(step.action()).append(" ")
                            .append(step.target() != null ? step.target() : "")
                            .append(step.value() != null && !step.value().isBlank() ? " -> '" + step.value() + "'" : "")
                            .append("\n");
                }
            }
        }

        sb.append("\nINSTRUCTIONS:\n");
        sb.append("Generate a structured failure analysis and bug report matching Table 4.3 specification.\n");
        sb.append("Return ONLY a JSON object with these exact fields:\n");
        sb.append("{\n");
        sb.append("  \"title\": \"Concise one-line summary of the bug\",\n");
        sb.append("  \"severity\": \"LOW | MEDIUM | HIGH | CRITICAL\",\n");
        sb.append("  \"description\": \"Detailed architectural explanation of what failed and why\",\n");
        sb.append("  \"stepsToReproduce\": \"Numbered step-by-step reproduction instructions\",\n");
        sb.append("  \"expectedResult\": \"What should have happened\",\n");
        sb.append("  \"actualResult\": \"What actually occurred (including UI error messages or rejected state)\",\n");
        sb.append("  \"possibleArea\": \"Suspected component, service, or application module\"\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String executeLlmRequest(String prompt) {
        String effectiveEndpoint = (endpoint != null && !endpoint.isBlank()) ? endpoint.trim() : "";

        // Check if Gemini
        if (effectiveEndpoint.contains("generativelanguage.googleapis.com") ||
                (effectiveEndpoint.isEmpty() && model != null && model.toLowerCase().contains("gemini"))) {
            String geminiUrl = !effectiveEndpoint.isEmpty()
                    ? effectiveEndpoint
                    : "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            Map<String, Object> payload = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "responseMimeType", "application/json"
                    )
            );

            String responseBody = restClient.post()
                    .uri(geminiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return extractGeminiText(responseBody);
        } else {
            // OpenAI compatible
            String openAiUrl = !effectiveEndpoint.isEmpty()
                    ? effectiveEndpoint
                    : "https://api.openai.com/v1/chat/completions";

            Map<String, Object> payload = Map.of(
                    "model", model != null ? model : "gpt-4o-mini",
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are an automated bug analysis engine that outputs only JSON objects."),
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String responseBody = restClient.post()
                    .uri(openAiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            return extractOpenAiText(responseBody);
        }
    }

    private String extractGeminiText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!textNode.isMissingNode()) {
                return textNode.asText();
            }
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
        }
        return responseBody;
    }

    private String extractOpenAiText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (!contentNode.isMissingNode()) {
                return contentNode.asText();
            }
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", e.getMessage());
        }
        return responseBody;
    }

    // -------------------------------------------------------------------------
    // Deterministic JSON Validation & Entity Mapping
    // -------------------------------------------------------------------------

    public BugReport parseAndValidateLlmResponse(String rawJson, Long executionId, ExecutionResponseDto execution) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }

        String cleaned = sanitizeJsonString(rawJson);
        GeneratedBugReportDto dto;
        try {
            dto = objectMapper.readValue(cleaned, GeneratedBugReportDto.class);
        } catch (Exception e) {
            log.warn("Jackson failed to deserialize LLM Bug Report JSON: {}. Cleaned content: {}", e.getMessage(), cleaned);
            return null;
        }

        if (dto == null || dto.title() == null || dto.title().isBlank()) {
            return null;
        }

        BugReport report = new BugReport();
        report.setExecutionId(executionId);
        report.setTitle(dto.title().trim());
        report.setSeverity(parseSeverity(dto.severity()));
        report.setDescription(dto.description() != null ? dto.description().trim() : "");
        report.setStepsToReproduce(dto.stepsToReproduce() != null ? dto.stepsToReproduce().trim() : "");
        report.setExpectedResult(dto.expectedResult() != null ? dto.expectedResult().trim() : "");
        report.setActualResult(dto.actualResult() != null ? dto.actualResult().trim() : "");
        report.setPossibleArea(dto.possibleArea() != null ? dto.possibleArea().trim() : "");
        report.setEvidencePath(execution != null ? execution.screenshotPath() : null);
        report.setCreatedAt(LocalDateTime.now());

        return report;
    }

    private String sanitizeJsonString(String input) {
        String trimmed = input.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private BugSeverity parseSeverity(String severityStr) {
        if (severityStr == null || severityStr.isBlank()) {
            return BugSeverity.MEDIUM;
        }
        try {
            return BugSeverity.valueOf(severityStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BugSeverity.MEDIUM;
        }
    }

    // -------------------------------------------------------------------------
    // Deterministic Rule-Based Bug Synthesis (Fallback Engine)
    // -------------------------------------------------------------------------

    public BugReport generateDeterministicBugReport(Long executionId, ExecutionResponseDto execution, TestCaseDto testCase) {
        String targetUrl = (execution != null && execution.targetUrl() != null && !execution.targetUrl().isBlank())
                ? execution.targetUrl()
                : "http://localhost:3000";

        String errorMessage = (execution != null && execution.errorMessage() != null)
                ? execution.errorMessage()
                : "";

        String screenshotPath = (execution != null && execution.screenshotPath() != null)
                ? execution.screenshotPath()
                : "screenshots/exec-" + executionId + ".png";

        boolean isAuthOrBugMode = errorMessage.contains("BUG_MODE")
                || errorMessage.toLowerCase().contains("authentication")
                || errorMessage.toLowerCase().contains("login")
                || (testCase != null && testCase.name() != null && testCase.name().toLowerCase().contains("login"));

        BugReport report = new BugReport();
        report.setExecutionId(executionId);
        report.setEvidencePath(screenshotPath);
        report.setCreatedAt(LocalDateTime.now());

        if (isAuthOrBugMode) {
            report.setTitle("Authentication Failure: Valid Credentials Rejected Under BUG_MODE");
            report.setSeverity(BugSeverity.HIGH);
            report.setDescription("Automated test execution failed during user authentication workflow against " + targetUrl +
                    ". Although valid credentials were submitted, the application returned an unexpected authentication error banner. " +
                    "This defect occurs when the application has BUG_MODE active or auth-service rejects legitimate tokens.");

            report.setStepsToReproduce(buildReproductionSteps(targetUrl, testCase));
            report.setExpectedResult("User credentials are accepted and the browser navigates to /dashboard with active session.");
            report.setActualResult(!errorMessage.isBlank() ? errorMessage : "Login failed: Authentication service returned an unexpected error (BUG_MODE=true).");
            report.setPossibleArea("Authentication Controller & Service (/api/auth/login or demo-target-app BUG_MODE toggle)");
        } else {
            String shortError = errorMessage.length() > 80 ? errorMessage.substring(0, 80) + "..." : errorMessage;
            report.setTitle(!errorMessage.isBlank() ? "Execution Failure: " + shortError : "Test Execution #" + executionId + " Assertion Failure");
            report.setSeverity(errorMessage.toLowerCase().contains("timeout") ? BugSeverity.HIGH : BugSeverity.MEDIUM);
            report.setDescription("Automated Playwright test execution #" + executionId + " failed against target application " + targetUrl +
                    ". The test step encountered an assertion failure or missing DOM element. Details: " +
                    (!errorMessage.isBlank() ? errorMessage : "Element was not visible within expected timeout."));

            report.setStepsToReproduce(buildReproductionSteps(targetUrl, testCase));
            report.setExpectedResult(testCase != null && testCase.expectedResult() != null ? testCase.expectedResult() : "Target element should be visible and step assertions should pass.");
            report.setActualResult(!errorMessage.isBlank() ? errorMessage : "Step execution failed with unhandled DOM error.");
            report.setPossibleArea("Frontend UI Component / Page Layout");
        }

        return report;
    }

    private String buildReproductionSteps(String targetUrl, TestCaseDto testCase) {
        if (testCase != null && testCase.steps() != null && !testCase.steps().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (TestStepDto step : testCase.steps()) {
                sb.append(step.stepOrder()).append(". ")
                        .append(step.action()).append(" ")
                        .append(step.target() != null ? step.target() : "");
                if (step.value() != null && !step.value().isBlank()) {
                    sb.append(" with value '").append(step.value()).append("'");
                }
                sb.append("\n");
            }
            sb.append(testCase.steps().size() + 1).append(". Observe error/assertion failure");
            return sb.toString();
        }

        return "1. Navigate to " + targetUrl + "/login\n" +
                "2. Enter valid registered email (test@example.com) into #email\n" +
                "3. Enter valid password (password123) into #password\n" +
                "4. Click submit button (button#login-submit)\n" +
                "5. Observe error banner displayed on screen";
    }
}

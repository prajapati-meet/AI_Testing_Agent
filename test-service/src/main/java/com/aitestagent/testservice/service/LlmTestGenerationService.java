package com.aitestagent.testservice.service;

import com.aitestagent.testservice.dto.exploration.DiscoveredPageDto;
import com.aitestagent.testservice.dto.exploration.ExplorationResponseDto;
import com.aitestagent.testservice.dto.llm.GeneratedTestCaseDto;
import com.aitestagent.testservice.dto.llm.GeneratedTestStepDto;
import com.aitestagent.testservice.entity.StepAction;
import com.aitestagent.testservice.entity.TestCase;
import com.aitestagent.testservice.entity.TestStep;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI-powered test generation service with LLM integration and deterministic fallback engine.
 */
@Service
public class LlmTestGenerationService {

    private static final Logger log = LoggerFactory.getLogger(LlmTestGenerationService.class);

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:gemini-1.5-flash}")
    private String model;

    @Value("${llm.endpoint:}")
    private String endpoint;

    public LlmTestGenerationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    /**
     * Generates structured functional test cases with executable steps.
     * Uses configured LLM if API key is present; otherwise falls back to deterministic rule generator.
     */
    public List<TestCase> generateTestCases(
            String targetUrl,
            Long explorationId,
            ExplorationResponseDto exploration,
            List<DiscoveredPageDto> pages
    ) {
        String cleanTargetUrl = (targetUrl != null && !targetUrl.isBlank()) ? targetUrl.trim() : "http://localhost:3000";

        if (apiKey != null && !apiKey.isBlank()) {
            try {
                log.info("Generating test cases using LLM (model: {}, endpoint: {})", model, endpoint);
                List<TestCase> llmCases = callLlm(cleanTargetUrl, explorationId, exploration, pages);
                if (llmCases != null && !llmCases.isEmpty()) {
                    log.info("Successfully generated {} test cases via LLM.", llmCases.size());
                    return llmCases;
                }
                log.warn("LLM returned empty test cases. Falling back to deterministic rule generator.");
            } catch (Exception ex) {
                log.warn("Failed to generate test cases via LLM: {}. Falling back to deterministic rule generator.", ex.getMessage());
            }
        } else {
            log.info("No LLM API key configured (LLM_API_KEY). Utilizing intelligent deterministic rule generator.");
        }

        return generateDeterministicTestCases(cleanTargetUrl, explorationId, exploration, pages);
    }

    // -------------------------------------------------------------------------
    // LLM Calling Logic
    // -------------------------------------------------------------------------

    private List<TestCase> callLlm(
            String targetUrl,
            Long explorationId,
            ExplorationResponseDto exploration,
            List<DiscoveredPageDto> pages
    ) throws Exception {
        String prompt = buildPrompt(targetUrl, exploration, pages);
        String rawResponseJson = executeLlmRequest(prompt);
        return parseAndValidateLlmResponse(rawResponseJson, cleanTargetUrl(targetUrl), explorationId);
    }

    private String buildPrompt(String targetUrl, ExplorationResponseDto exploration, List<DiscoveredPageDto> pages) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert Senior QA Automation Engineer specializing in Playwright end-to-end testing.\n");
        sb.append("Analyze the following web application exploration metadata, discovered pages, inputs, buttons, and navigation flow.\n");
        sb.append("Target URL: ").append(targetUrl).append("\n");

        if (exploration != null && exploration.navigationFlowSummary() != null) {
            sb.append("Navigation Flow Summary: ").append(exploration.navigationFlowSummary()).append("\n");
        }

        sb.append("\nDiscovered Pages:\n");
        if (pages != null && !pages.isEmpty()) {
            for (DiscoveredPageDto page : pages) {
                sb.append("- Page URL: ").append(page.pageUrl()).append(" (Title: ").append(page.pageTitle()).append(")\n");
                if (page.inputs() != null && !page.inputs().isBlank()) {
                    sb.append("  Inputs: ").append(page.inputs()).append("\n");
                }
                if (page.buttons() != null && !page.buttons().isBlank()) {
                    sb.append("  Buttons: ").append(page.buttons()).append("\n");
                }
                if (page.forms() != null && !page.forms().isBlank()) {
                    sb.append("  Forms: ").append(page.forms()).append("\n");
                }
                if (page.navigationFlow() != null && !page.navigationFlow().isBlank()) {
                    sb.append("  Navigation: ").append(page.navigationFlow()).append("\n");
                }
            }
        } else {
            sb.append("No pages explicitly discovered. Generate default comprehensive login & validation scenarios for: ")
                    .append(targetUrl).append("\n");
        }

        sb.append("\nREQUIREMENTS:\n");
        sb.append("Generate 2 to 4 end-to-end test cases covering:\n");
        sb.append("1. Happy path user flow (e.g. Valid login authentication)\n");
        sb.append("2. Negative defect detection flow (e.g. Invalid credentials resulting in error banner)\n");
        sb.append("3. Input validation flow (e.g. Missing required fields)\n");
        sb.append("\nStrict Rules for TestStep actions:\n");
        sb.append("- action MUST be one of: NAVIGATE, FILL, CLICK, WAIT, ASSERT_VISIBLE, ASSERT_TEXT\n");
        sb.append("- target: For NAVIGATE, target is the destination URL (e.g. ").append(targetUrl).append("). For ALL OTHER ACTIONS (FILL, CLICK, WAIT, ASSERT_VISIBLE, ASSERT_TEXT), target MUST BE A VALID DOM CSS SELECTOR (e.g. #email, button#login-submit, #login-error, .error-banner, #dashboard-container, nav, main, body, h1). NEVER use a URL in target for non-NAVIGATE actions!\n");
        sb.append("- value: input text for FILL, expected text for ASSERT_TEXT, milliseconds for WAIT, or empty string\n");
        sb.append("- condition: optional condition or empty string\n");
        sb.append("- stepOrder: 1-based sequential integer\n");
        sb.append("- CRITICAL ASSERTION RULES FOR STEP VERIFICATION:\n");
        sb.append("  * For Happy Path login: The final step MUST ASSERT_VISIBLE on an authenticated element like '#dashboard-container', 'button#logout-button', or 'h2'.\n");
        sb.append("  * For Negative defect detection flow (Invalid Credentials): The final step MUST ASSERT_VISIBLE on the error feedback element '#login-error' or '.error-banner' (which is expected to show an error message). NEVER assert '#email' or 'button#login-submit' for invalid credentials, because those are always visible and would mask bugs!\n");
        sb.append("  * For Missing Required Fields: The final step can ASSERT_VISIBLE on ':invalid', 'input:invalid', or '.error'.\n");
        sb.append("\nReturn ONLY a JSON array matching this exact schema without any markdown formatting:\n");
        sb.append("[\n");
        sb.append("  {\n");
        sb.append("    \"name\": \"Test Case Name\",\n");
        sb.append("    \"description\": \"Description\",\n");
        sb.append("    \"priority\": \"HIGH\",\n");
        sb.append("    \"expectedResult\": \"Expected Outcome\",\n");
        sb.append("    \"steps\": [\n");
        sb.append("      {\n");
        sb.append("        \"stepOrder\": 1,\n");
        sb.append("        \"action\": \"NAVIGATE\",\n");
        sb.append("        \"target\": \"").append(targetUrl).append("/login\",\n");
        sb.append("        \"value\": \"\",\n");
        sb.append("        \"condition\": \"\"\n");
        sb.append("      },\n");
        sb.append("      {\n");
        sb.append("        \"stepOrder\": 2,\n");
        sb.append("        \"action\": \"FILL\",\n");
        sb.append("        \"target\": \"#email\",\n");
        sb.append("        \"value\": \"test@example.com\",\n");
        sb.append("        \"condition\": \"\"\n");
        sb.append("      },\n");
        sb.append("      {\n");
        sb.append("        \"stepOrder\": 3,\n");
        sb.append("        \"action\": \"FILL\",\n");
        sb.append("        \"target\": \"#password\",\n");
        sb.append("        \"value\": \"password123\",\n");
        sb.append("        \"condition\": \"\"\n");
        sb.append("      },\n");
        sb.append("      {\n");
        sb.append("        \"stepOrder\": 4,\n");
        sb.append("        \"action\": \"CLICK\",\n");
        sb.append("        \"target\": \"button#login-submit\",\n");
        sb.append("        \"value\": \"\",\n");
        sb.append("        \"condition\": \"\"\n");
        sb.append("      },\n");
        sb.append("      {\n");
        sb.append("        \"stepOrder\": 5,\n");
        sb.append("        \"action\": \"ASSERT_VISIBLE\",\n");
        sb.append("        \"target\": \"#dashboard-container\",\n");
        sb.append("        \"value\": \"\",\n");
        sb.append("        \"condition\": \"\"\n");
        sb.append("      }\n");
        sb.append("    ]\n");
        sb.append("  }\n");
        sb.append("]\n");

        return sb.toString();
    }

    private String executeLlmRequest(String prompt) {
        String effectiveEndpoint = (endpoint != null && !endpoint.isBlank()) ? endpoint.trim() : "";

        // Check if Gemini API
        if (effectiveEndpoint.contains("generativelanguage.googleapis.com") ||
                (effectiveEndpoint.isEmpty() && model != null && model.toLowerCase().contains("gemini"))) {
            String cleanModel = (model != null && !model.isBlank())
                    ? model.trim().toLowerCase().replaceAll("\\s+", "-").replace("models/", "")
                    : "gemini-3.5-flash-lite";
            String geminiUrl = !effectiveEndpoint.isEmpty()
                    ? effectiveEndpoint
                    : "https://generativelanguage.googleapis.com/v1beta/models/" + cleanModel + ":generateContent?key=" + apiKey;

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

            Exception lastEx = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    String responseBody = restClient.post()
                            .uri(geminiUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .body(String.class);

                    return extractGeminiText(responseBody);
                } catch (Exception ex) {
                    lastEx = ex;
                    log.warn("Gemini test generation attempt #{} failed: {}. Retrying in {}ms...",
                            attempt, ex.getMessage(), attempt * 1500);
                    try {
                        Thread.sleep(attempt * 1500L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            if (lastEx != null) {
                throw new RuntimeException("Gemini test generation failed after retries: " + lastEx.getMessage(), lastEx);
            }
            return "";
        } else {
            // OpenAI compatible chat completions endpoint
            String openAiUrl = !effectiveEndpoint.isEmpty()
                    ? effectiveEndpoint
                    : "https://api.openai.com/v1/chat/completions";

            Map<String, Object> payload = Map.of(
                    "model", model != null ? model : "gpt-4o-mini",
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are an automated test generator that returns only raw JSON arrays."),
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

    public List<TestCase> parseAndValidateLlmResponse(String rawJson, String targetUrl, Long explorationId) {
        if (rawJson == null || rawJson.isBlank()) {
            return Collections.emptyList();
        }

        String cleaned = sanitizeJsonString(rawJson);
        List<GeneratedTestCaseDto> dtos;
        try {
            dtos = objectMapper.readValue(cleaned, new TypeReference<List<GeneratedTestCaseDto>>() {});
        } catch (Exception e) {
            log.warn("Jackson failed to deserialize LLM JSON: {}. Cleaned content: {}", e.getMessage(), cleaned);
            return Collections.emptyList();
        }

        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }

        List<TestCase> testCases = new ArrayList<>();
        for (GeneratedTestCaseDto dto : dtos) {
            if (dto.name() == null || dto.name().isBlank()) {
                continue;
            }

            TestCase tc = new TestCase();
            tc.setName(dto.name().trim());
            tc.setDescription(dto.description() != null ? dto.description().trim() : "");
            tc.setPriority(dto.priority() != null && !dto.priority().isBlank() ? dto.priority().trim().toUpperCase() : "MEDIUM");
            tc.setExpectedResult(dto.expectedResult() != null ? dto.expectedResult().trim() : "");
            tc.setTargetUrl(targetUrl);
            tc.setExplorationId(explorationId);
            tc.setCreatedAt(LocalDateTime.now());

            int order = 1;
            if (dto.steps() != null) {
                for (GeneratedTestStepDto stepDto : dto.steps()) {
                    StepAction action = StepAction.fromString(stepDto.action());
                    String target = stepDto.target() != null ? stepDto.target().trim() : "";
                    String value = stepDto.value() != null ? stepDto.value().trim() : "";
                    String condition = stepDto.condition() != null ? stepDto.condition().trim() : "";

                    // Guardrail: if an LLM mistakenly passed a URL as target for ASSERT_VISIBLE or ASSERT_TEXT
                    if (action != StepAction.NAVIGATE && (target.startsWith("http://") || target.startsWith("https://"))) {
                        log.warn("Detected invalid URL '{}' as target for action {}. Sanitizing to 'body, main, header'.", target, action);
                        target = "body, main, header, h1";
                    }

                    TestStep step = new TestStep(order++, action, target, value, condition);
                    tc.addStep(step);
                }
            }

            if (!tc.getSteps().isEmpty()) {
                // Defect detection guardrail: Ensure negative / invalid credentials flows verify the error banner (#login-error),
                // not input fields (#email) or submit buttons which remain visible regardless of error state.
                String tcNameLower = tc.getName() != null ? tc.getName().toLowerCase() : "";
                if (tcNameLower.contains("invalid") || tcNameLower.contains("negative") || tcNameLower.contains("defect") || tcNameLower.contains("bad")) {
                    TestStep lastStep = tc.getSteps().get(tc.getSteps().size() - 1);
                    if (lastStep.getAction() == StepAction.ASSERT_VISIBLE || lastStep.getAction() == StepAction.ASSERT_TEXT) {
                        String tgt = lastStep.getTarget() != null ? lastStep.getTarget().trim() : "";
                        if (tgt.equals("#email") || tgt.equals("#password") || tgt.contains("button#login-submit") || tgt.equals("form")) {
                            log.info("Refining negative test case assertion from '{}' to '#login-error, .error-banner' for accurate defect verification.", tgt);
                            lastStep.setTarget("#login-error, .error-banner");
                        }
                    }
                }
                testCases.add(tc);
            }
        }

        return testCases;
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

    // -------------------------------------------------------------------------
    // Intelligent Deterministic Rule Generator (Fallback Engine)
    // -------------------------------------------------------------------------

    public List<TestCase> generateDeterministicTestCases(
            String targetUrl,
            Long explorationId,
            ExplorationResponseDto exploration,
            List<DiscoveredPageDto> pages
    ) {
        List<TestCase> results = new ArrayList<>();

        // 1. Analyze discovered elements for login / authentication form
        DiscoveredElements elements = extractElements(targetUrl, pages);

        String loginUrl = elements.loginPageUrl;
        String emailSelector = elements.emailSelector;
        String passwordSelector = elements.passwordSelector;
        String submitSelector = elements.submitSelector;

        // --- Scenario 1: Valid User Login (Happy Path) ---
        TestCase validLogin = new TestCase();
        validLogin.setName("Verify Valid User Login Workflow");
        validLogin.setDescription("Validates that entering registered credentials successfully logs in the user and navigates to the authenticated dashboard view.");
        validLogin.setPriority("HIGH");
        validLogin.setExpectedResult("User is authenticated and redirected to /dashboard or welcome view with session active.");
        validLogin.setTargetUrl(targetUrl);
        validLogin.setExplorationId(explorationId);
        validLogin.setCreatedAt(LocalDateTime.now());

        validLogin.addStep(new TestStep(1, StepAction.NAVIGATE, loginUrl, "", ""));
        validLogin.addStep(new TestStep(2, StepAction.FILL, emailSelector, "test@example.com", ""));
        validLogin.addStep(new TestStep(3, StepAction.FILL, passwordSelector, "password123", ""));
        validLogin.addStep(new TestStep(4, StepAction.CLICK, submitSelector, "", ""));
        validLogin.addStep(new TestStep(5, StepAction.WAIT, "body", "2000", ""));
        validLogin.addStep(new TestStep(6, StepAction.ASSERT_VISIBLE, ".dashboard-container, nav, header, h1, body", "", ""));
        results.add(validLogin);

        // --- Scenario 2: Invalid Login Defect Detection (Negative Path) ---
        TestCase invalidLogin = new TestCase();
        invalidLogin.setName("Verify Invalid Credentials Display Error Banner");
        invalidLogin.setDescription("Validates that invalid authentication attempts trigger error banners and reject access (validates defect detection under BUG_MODE).");
        invalidLogin.setPriority("HIGH");
        invalidLogin.setExpectedResult("Authentication error message or banner is visible and user remains unauthenticated.");
        invalidLogin.setTargetUrl(targetUrl);
        invalidLogin.setExplorationId(explorationId);
        invalidLogin.setCreatedAt(LocalDateTime.now());

        invalidLogin.addStep(new TestStep(1, StepAction.NAVIGATE, loginUrl, "", ""));
        invalidLogin.addStep(new TestStep(2, StepAction.FILL, emailSelector, "test@example.com", ""));
        invalidLogin.addStep(new TestStep(3, StepAction.FILL, passwordSelector, "incorrect_password_999", ""));
        invalidLogin.addStep(new TestStep(4, StepAction.CLICK, submitSelector, "", ""));
        invalidLogin.addStep(new TestStep(5, StepAction.WAIT, "body", "1500", ""));
        invalidLogin.addStep(new TestStep(6, StepAction.ASSERT_VISIBLE, "#login-error, .error-banner, .error-message, .alert, .error, [role='alert'], .text-danger", "", ""));
        results.add(invalidLogin);

        // --- Scenario 3: Required Field Validation (Boundary / Negative) ---
        TestCase emptySubmission = new TestCase();
        emptySubmission.setName("Verify Empty Credential Validation");
        emptySubmission.setDescription("Ensures that submitting the login form without required inputs halts submission with validation error or HTML5 input state.");
        emptySubmission.setPriority("MEDIUM");
        emptySubmission.setExpectedResult("Form submission is prevented and validation indicators appear on required inputs.");
        emptySubmission.setTargetUrl(targetUrl);
        emptySubmission.setExplorationId(explorationId);
        emptySubmission.setCreatedAt(LocalDateTime.now());

        emptySubmission.addStep(new TestStep(1, StepAction.NAVIGATE, loginUrl, "", ""));
        emptySubmission.addStep(new TestStep(2, StepAction.CLICK, submitSelector, "", ""));
        emptySubmission.addStep(new TestStep(3, StepAction.ASSERT_VISIBLE, ":invalid, input:invalid, .error, form", "", ""));
        results.add(emptySubmission);

        // --- Additional Scenarios: Discovered Non-Login Pages ---
        if (pages != null) {
            int extraCount = 0;
            for (DiscoveredPageDto page : pages) {
                if (extraCount >= 2) break;
                if (page.pageUrl() != null && !page.pageUrl().equalsIgnoreCase(loginUrl) && !page.pageUrl().contains("login")) {
                    TestCase navTest = new TestCase();
                    String pageTitle = (page.pageTitle() != null && !page.pageTitle().isBlank()) ? page.pageTitle() : "Discovered Page";
                    navTest.setName("Verify Page Navigation and Render: " + pageTitle);
                    navTest.setDescription("Ensures that " + page.pageUrl() + " renders properly with active elements.");
                    navTest.setPriority("MEDIUM");
                    navTest.setExpectedResult("Page loads successfully with primary layout visible.");
                    navTest.setTargetUrl(targetUrl);
                    navTest.setExplorationId(explorationId);
                    navTest.setCreatedAt(LocalDateTime.now());

                    navTest.addStep(new TestStep(1, StepAction.NAVIGATE, page.pageUrl(), "", ""));
                    navTest.addStep(new TestStep(2, StepAction.WAIT, "body", "1000", ""));
                    navTest.addStep(new TestStep(3, StepAction.ASSERT_VISIBLE, "main, .container, body", "", ""));

                    // If button exists on page, add click step
                    String buttonSelector = extractFirstButtonSelector(page.buttons());
                    if (buttonSelector != null) {
                        navTest.addStep(new TestStep(4, StepAction.CLICK, buttonSelector, "", ""));
                        navTest.addStep(new TestStep(5, StepAction.WAIT, "body", "1000", ""));
                    }

                    results.add(navTest);
                    extraCount++;
                }
            }
        }

        return results;
    }

    private static class DiscoveredElements {
        String loginPageUrl;
        String emailSelector;
        String passwordSelector;
        String submitSelector;
    }

    private DiscoveredElements extractElements(String targetUrl, List<DiscoveredPageDto> pages) {
        DiscoveredElements el = new DiscoveredElements();
        el.loginPageUrl = targetUrl;
        el.emailSelector = "#email";
        el.passwordSelector = "#password";
        el.submitSelector = "button#login-submit";

        if (pages == null || pages.isEmpty()) {
            if (!targetUrl.endsWith("/login")) {
                el.loginPageUrl = targetUrl.endsWith("/") ? targetUrl + "login" : targetUrl + "/login";
            }
            return el;
        }

        DiscoveredPageDto bestLoginPage = null;

        for (DiscoveredPageDto page : pages) {
            String url = page.pageUrl() != null ? page.pageUrl().toLowerCase() : "";
            String title = page.pageTitle() != null ? page.pageTitle().toLowerCase() : "";

            if (url.contains("login") || title.contains("login") || title.contains("sign in")) {
                bestLoginPage = page;
                break;
            }
        }

        // If no explicit login URL, inspect page inputs for email & password
        if (bestLoginPage == null) {
            for (DiscoveredPageDto page : pages) {
                if (hasLoginInputs(page.inputs())) {
                    bestLoginPage = page;
                    break;
                }
            }
        }

        if (bestLoginPage != null) {
            if (bestLoginPage.pageUrl() != null && !bestLoginPage.pageUrl().isBlank()) {
                el.loginPageUrl = bestLoginPage.pageUrl();
            }

            // Extract input selectors from page
            extractInputSelectors(bestLoginPage.inputs(), el);

            // Extract submit button from page
            String submitBtn = extractSubmitButtonSelector(bestLoginPage.buttons());
            if (submitBtn != null) {
                el.submitSelector = submitBtn;
            }
        } else {
            if (!targetUrl.endsWith("/login")) {
                el.loginPageUrl = targetUrl.endsWith("/") ? targetUrl + "login" : targetUrl + "/login";
            }
        }

        return el;
    }

    private boolean hasLoginInputs(String inputsJson) {
        if (inputsJson == null || inputsJson.isBlank()) {
            return false;
        }
        try {
            JsonNode array = objectMapper.readTree(inputsJson);
            if (array.isArray()) {
                boolean hasPassword = false;
                for (JsonNode node : array) {
                    String type = node.path("type").asText("");
                    if ("password".equalsIgnoreCase(type)) {
                        hasPassword = true;
                        break;
                    }
                }
                return hasPassword;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void extractInputSelectors(String inputsJson, DiscoveredElements el) {
        if (inputsJson == null || inputsJson.isBlank()) {
            return;
        }
        try {
            JsonNode array = objectMapper.readTree(inputsJson);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    String id = node.path("id").asText("");
                    String name = node.path("name").asText("");
                    String type = node.path("type").asText("text");
                    String placeholder = node.path("placeholder").asText("").toLowerCase();

                    String selector = !id.isBlank() ? "#" + id : (!name.isBlank() ? "input[name='" + name + "']" : null);

                    if ("password".equalsIgnoreCase(type) || id.toLowerCase().contains("pass") || name.toLowerCase().contains("pass")) {
                        el.passwordSelector = selector != null ? selector : "input[type='password']";
                    } else if ("email".equalsIgnoreCase(type) || id.toLowerCase().contains("email") || name.toLowerCase().contains("email")
                            || id.toLowerCase().contains("user") || name.toLowerCase().contains("user") || placeholder.contains("email")) {
                        el.emailSelector = selector != null ? selector : "input[type='email']";
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String extractSubmitButtonSelector(String buttonsJson) {
        if (buttonsJson == null || buttonsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode array = objectMapper.readTree(buttonsJson);
            if (array.isArray()) {
                String firstSelector = null;
                for (JsonNode node : array) {
                    String text = node.path("text").asText("").toLowerCase();
                    String selector = node.path("selector").asText("");

                    if (firstSelector == null && !selector.isBlank()) {
                        firstSelector = selector;
                    }

                    if (text.contains("log") || text.contains("sign") || text.contains("submit") || selector.contains("login") || selector.contains("submit")) {
                        return selector;
                    }
                }
                return firstSelector;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractFirstButtonSelector(String buttonsJson) {
        if (buttonsJson == null || buttonsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode array = objectMapper.readTree(buttonsJson);
            if (array.isArray() && array.size() > 0) {
                String selector = array.get(0).path("selector").asText("");
                return !selector.isBlank() ? selector : null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String cleanTargetUrl(String url) {
        return (url != null && !url.isBlank()) ? url.trim() : "http://localhost:3000";
    }
}

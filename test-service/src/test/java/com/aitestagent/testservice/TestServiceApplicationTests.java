package com.aitestagent.testservice;

import com.aitestagent.testservice.dto.GenerateTestsRequest;
import com.aitestagent.testservice.dto.TestCaseResponse;
import com.aitestagent.testservice.dto.TestStepResponse;
import com.aitestagent.testservice.dto.exploration.DiscoveredPageDto;
import com.aitestagent.testservice.entity.StepAction;
import com.aitestagent.testservice.entity.TestCase;
import com.aitestagent.testservice.service.LlmTestGenerationService;
import com.aitestagent.testservice.service.TestCaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TestServiceApplicationTests {

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private LlmTestGenerationService llmTestGenerationService;

    @Test
    @DisplayName("Context loads and getAllTestCases seeds and returns test cases with steps")
    void contextLoadsAndReturnsTestCases() {
        List<TestCaseResponse> testCases = testCaseService.getAllTestCases();
        assertThat(testCases).isNotEmpty();

        TestCaseResponse firstCase = testCases.get(0);
        assertThat(firstCase.name()).isNotBlank();
        assertThat(firstCase.steps()).isNotEmpty();

        TestStepResponse firstStep = firstCase.steps().get(0);
        assertThat(firstStep.stepOrder()).isEqualTo(1);
        assertThat(firstStep.action()).isEqualTo("NAVIGATE");
        assertThat(firstStep.target()).isNotBlank();
    }

    @Test
    @DisplayName("generateTestCases creates structured happy-path and negative test cases with steps")
    void testGenerateTestCasesWithFallbackEngine() {
        GenerateTestsRequest request = new GenerateTestsRequest(null, "http://localhost:3000");
        List<TestCaseResponse> generated = testCaseService.generateTestCases(request);

        assertThat(generated).hasSizeGreaterThanOrEqualTo(3);

        // Verify Happy Path scenario
        TestCaseResponse validLogin = generated.stream()
                .filter(tc -> tc.name().contains("Valid"))
                .findFirst()
                .orElse(null);
        assertThat(validLogin).isNotNull();
        assertThat(validLogin.priority()).isEqualTo("HIGH");
        assertThat(validLogin.steps()).hasSizeGreaterThanOrEqualTo(4);

        // Verify Negative / Bug Mode detection scenario
        TestCaseResponse invalidLogin = generated.stream()
                .filter(tc -> tc.name().contains("Invalid"))
                .findFirst()
                .orElse(null);
        assertThat(invalidLogin).isNotNull();
        assertThat(invalidLogin.priority()).isEqualTo("HIGH");
        assertThat(invalidLogin.steps().stream().anyMatch(s -> s.action().equals("FILL"))).isTrue();
        assertThat(invalidLogin.steps().stream().anyMatch(s -> s.action().equals("ASSERT_VISIBLE"))).isTrue();

        // Verify Validation scenario
        TestCaseResponse emptyValidation = generated.stream()
                .filter(tc -> tc.name().contains("Empty") || tc.name().contains("Validation"))
                .findFirst()
                .orElse(null);
        assertThat(emptyValidation).isNotNull();
    }

    @Test
    @DisplayName("Deterministic rule generator extracts and uses discovered DOM input selectors")
    void testDeterministicRuleGeneratorWithDiscoveredPages() {
        String inputsJson = """
                [
                    {"id":"user-email","name":"email","type":"email","placeholder":"Enter email"},
                    {"id":"user-password","name":"password","type":"password","placeholder":"Enter password"}
                ]
                """;
        String buttonsJson = """
                [
                    {"text":"Sign In Now","selector":"button#custom-login-submit"}
                ]
                """;

        DiscoveredPageDto page = new DiscoveredPageDto(
                1L,
                100L,
                "http://localhost:3000/login",
                "Login Page",
                "[]",
                buttonsJson,
                inputsJson,
                "[]",
                "Login -> Dashboard"
        );

        List<TestCase> testCases = llmTestGenerationService.generateDeterministicTestCases(
                "http://localhost:3000",
                100L,
                null,
                List.of(page)
        );

        assertThat(testCases).isNotEmpty();

        TestCase validCase = testCases.stream()
                .filter(tc -> tc.getName().contains("Valid"))
                .findFirst()
                .orElseThrow();

        // Ensure discovered selectors were leveraged
        assertThat(validCase.getSteps()).anyMatch(s -> "#user-email".equals(s.getTarget()));
        assertThat(validCase.getSteps()).anyMatch(s -> "#user-password".equals(s.getTarget()));
        assertThat(validCase.getSteps()).anyMatch(s -> "button#custom-login-submit".equals(s.getTarget()));
    }

    @Test
    @DisplayName("parseAndValidateLlmResponse sanitizes markdown code blocks and validates JSON schema")
    void testParseAndValidateLlmResponseWithMarkdownFence() {
        String rawLlmResponse = """
                ```json
                [
                  {
                    "name": "AI Validated Checkout Flow",
                    "description": "Tests product purchase and checkout completion",
                    "priority": "HIGH",
                    "expectedResult": "Order is confirmed with order ID",
                    "steps": [
                      {
                        "stepOrder": 1,
                        "action": "NAVIGATE",
                        "target": "http://localhost:3000/products",
                        "value": "",
                        "condition": ""
                      },
                      {
                        "stepOrder": 2,
                        "action": "CLICK",
                        "target": "button.add-to-cart",
                        "value": "",
                        "condition": ""
                      },
                      {
                        "stepOrder": 3,
                        "action": "ASSERT_VISIBLE",
                        "target": ".cart-badge",
                        "value": "",
                        "condition": ""
                      }
                    ]
                  }
                ]
                ```
                """;

        List<TestCase> parsed = llmTestGenerationService.parseAndValidateLlmResponse(
                rawLlmResponse,
                "http://localhost:3000",
                55L
        );

        assertThat(parsed).hasSize(1);
        TestCase tc = parsed.get(0);
        assertThat(tc.getName()).isEqualTo("AI Validated Checkout Flow");
        assertThat(tc.getPriority()).isEqualTo("HIGH");
        assertThat(tc.getSteps()).hasSize(3);
        assertThat(tc.getSteps().get(0).getAction()).isEqualTo(StepAction.NAVIGATE);
        assertThat(tc.getSteps().get(1).getAction()).isEqualTo(StepAction.CLICK);
        assertThat(tc.getSteps().get(2).getAction()).isEqualTo(StepAction.ASSERT_VISIBLE);
        assertThat(tc.getSteps().get(2).getTarget()).isEqualTo(".cart-badge");
    }

    @Test
    @DisplayName("getTestCaseById retrieves single test case with steps or throws when not found")
    void testGetTestCaseById() {
        List<TestCaseResponse> generated = testCaseService.generateTestCases(new GenerateTestsRequest(null, "http://localhost:3000"));
        assertThat(generated).isNotEmpty();

        Long id = generated.get(0).id();
        TestCaseResponse single = testCaseService.getTestCaseById(id);

        assertThat(single).isNotNull();
        assertThat(single.id()).isEqualTo(id);
        assertThat(single.steps()).isNotEmpty();
    }
}

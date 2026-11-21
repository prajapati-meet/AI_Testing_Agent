package com.aitestagent.testservice.service;

import com.aitestagent.testservice.dto.GenerateTestsRequest;
import com.aitestagent.testservice.dto.TestCaseResponse;
import com.aitestagent.testservice.entity.TestCase;
import com.aitestagent.testservice.repository.TestCaseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TestCaseService {

    public static final String DEFAULT_LOGIN_STEPS = """
            [
              {"stepOrder":1,"action":"NAVIGATE","target":"","value":"http://localhost:3000/login"},
              {"stepOrder":2,"action":"WAIT","target":"#email","value":""},
              {"stepOrder":3,"action":"FILL","target":"#email","value":"test@example.com"},
              {"stepOrder":4,"action":"FILL","target":"#password","value":"password123"},
              {"stepOrder":5,"action":"CLICK","target":"#login-submit","value":""},
              {"stepOrder":6,"action":"WAIT","target":"h2","value":""},
              {"stepOrder":7,"action":"ASSERT_TEXT","target":"h2","value":"Customer Account Dashboard"}
            ]
            """.trim();

    private final TestCaseRepository testCaseRepository;

    public TestCaseService(TestCaseRepository testCaseRepository) {
        this.testCaseRepository = testCaseRepository;
    }

    public List<TestCaseResponse> generateTestCases(GenerateTestsRequest request) {
        TestCase sample = new TestCase();
        sample.setName("Verify Login Workflow");
        sample.setDescription("Placeholder AI-generated test case for login page validation");
        sample.setPriority("HIGH");
        sample.setExpectedResult("User logs in with valid credentials and lands on /dashboard");
        sample.setSteps(DEFAULT_LOGIN_STEPS);
        sample.setCreatedAt(LocalDateTime.now());

        TestCase saved = testCaseRepository.save(sample);
        return List.of(mapToResponse(saved));
    }

    public List<TestCaseResponse> getAllTestCases() {
        List<TestCase> existing = testCaseRepository.findAll();
        if (existing.isEmpty()) {
            return List.of(new TestCaseResponse(
                    1L,
                    "Verify Login Workflow (Placeholder)",
                    "Ensure valid user credentials redirect to dashboard",
                    "HIGH",
                    "Redirected to /dashboard",
                    DEFAULT_LOGIN_STEPS,
                    LocalDateTime.now()
            ));
        }
        return existing.stream().map(this::mapToResponse).toList();
    }

    public TestCaseResponse getTestCaseById(Long id) {
        return testCaseRepository.findById(id)
                .map(this::mapToResponse)
                .orElseGet(() -> new TestCaseResponse(
                        id,
                        "Verify Login Workflow (Placeholder)",
                        "Ensure valid user credentials redirect to dashboard",
                        "HIGH",
                        "Redirected to /dashboard",
                        DEFAULT_LOGIN_STEPS,
                        LocalDateTime.now()
                ));
    }

    private TestCaseResponse mapToResponse(TestCase entity) {
        return new TestCaseResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPriority(),
                entity.getExpectedResult(),
                entity.getSteps() != null ? entity.getSteps() : DEFAULT_LOGIN_STEPS,
                entity.getCreatedAt()
        );
    }
}

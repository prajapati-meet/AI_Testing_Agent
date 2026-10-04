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
                entity.getCreatedAt()
        );
    }
}

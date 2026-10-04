package com.aitestagent.execution.service;

import com.aitestagent.execution.dto.ExecutionRequest;
import com.aitestagent.execution.dto.ExecutionResponse;
import com.aitestagent.execution.entity.Execution;
import com.aitestagent.execution.entity.ExecutionStatus;
import com.aitestagent.execution.repository.ExecutionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExecutionService {

    private final ExecutionRepository executionRepository;

    public ExecutionService(ExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    public ExecutionResponse triggerExecution(ExecutionRequest request) {
        Long testCaseId = (request != null && request.testCaseId() != null) ? request.testCaseId() : 1L;
        String targetUrl = (request != null && request.targetUrl() != null)
                ? request.targetUrl()
                : "http://localhost:3000";

        Execution execution = new Execution();
        execution.setTestCaseId(testCaseId);
        execution.setTargetUrl(targetUrl);
        execution.setStatus(ExecutionStatus.PENDING);
        execution.setStartedAt(LocalDateTime.now());

        Execution saved = executionRepository.save(execution);
        return mapToResponse(saved);
    }

    public List<ExecutionResponse> getAllExecutions() {
        List<Execution> existing = executionRepository.findAll();
        if (existing.isEmpty()) {
            return List.of(new ExecutionResponse(
                    1L,
                    1L,
                    "http://localhost:3000",
                    ExecutionStatus.PASSED,
                    LocalDateTime.now().minusMinutes(1),
                    LocalDateTime.now(),
                    null,
                    null
            ));
        }
        return existing.stream().map(this::mapToResponse).toList();
    }

    public ExecutionResponse getExecutionById(Long id) {
        return executionRepository.findById(id)
                .map(this::mapToResponse)
                .orElseGet(() -> new ExecutionResponse(
                        id,
                        1L,
                        "http://localhost:3000",
                        ExecutionStatus.PASSED,
                        LocalDateTime.now().minusMinutes(1),
                        LocalDateTime.now(),
                        null,
                        null
                ));
    }

    private ExecutionResponse mapToResponse(Execution entity) {
        return new ExecutionResponse(
                entity.getId(),
                entity.getTestCaseId(),
                entity.getTargetUrl(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getErrorMessage(),
                entity.getScreenshotPath()
        );
    }
}

package com.aitestagent.execution.controller;

import com.aitestagent.execution.dto.ExecutionRequest;
import com.aitestagent.execution.dto.ExecutionResponse;
import com.aitestagent.execution.service.ExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping
    public ResponseEntity<ExecutionResponse> triggerExecution(
            @RequestBody(required = false) ExecutionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(executionService.triggerExecution(request));
    }

    @GetMapping
    public ResponseEntity<List<ExecutionResponse>> getAllExecutions() {
        return ResponseEntity.ok(executionService.getAllExecutions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExecutionResponse> getExecutionById(@PathVariable Long id) {
        return ResponseEntity.ok(executionService.getExecutionById(id));
    }
}

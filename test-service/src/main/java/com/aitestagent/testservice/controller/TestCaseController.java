package com.aitestagent.testservice.controller;

import com.aitestagent.testservice.dto.GenerateTestsRequest;
import com.aitestagent.testservice.dto.TestCaseResponse;
import com.aitestagent.testservice.service.TestCaseService;
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
@RequestMapping("/api/tests")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<TestCaseResponse>> generateTests(
            @RequestBody(required = false) GenerateTestsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(testCaseService.generateTestCases(request));
    }

    @GetMapping
    public ResponseEntity<List<TestCaseResponse>> getAllTests() {
        return ResponseEntity.ok(testCaseService.getAllTestCases());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestCaseResponse> getTestById(@PathVariable Long id) {
        return ResponseEntity.ok(testCaseService.getTestCaseById(id));
    }
}

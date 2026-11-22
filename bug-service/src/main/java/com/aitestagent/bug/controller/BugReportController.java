package com.aitestagent.bug.controller;

import com.aitestagent.bug.dto.BugReportResponse;
import com.aitestagent.bug.service.BugReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@RestController
@RequestMapping("/api/bugs")
public class BugReportController {

    private final BugReportService bugReportService;

    public BugReportController(BugReportService bugReportService) {
        this.bugReportService = bugReportService;
    }

    @PostMapping("/analyze/{executionId}")
    public ResponseEntity<BugReportResponse> analyzeExecution(
            @PathVariable Long executionId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bugReportService.analyzeExecution(executionId, userEmail));
    }

    @GetMapping
    public ResponseEntity<List<BugReportResponse>> getAllBugs(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        return ResponseEntity.ok(bugReportService.getAllBugReports(userEmail));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BugReportResponse> getBugById(@PathVariable Long id) {
        return ResponseEntity.ok(bugReportService.getBugReportById(id));
    }
}

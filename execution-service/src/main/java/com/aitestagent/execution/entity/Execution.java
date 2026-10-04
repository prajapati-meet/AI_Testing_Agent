package com.aitestagent.execution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "executions")
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long testCaseId;

    @Column(nullable = false)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(length = 1000)
    private String errorMessage;

    private String screenshotPath;

    public Execution() {
    }

    public Execution(Long id, Long testCaseId, String targetUrl, ExecutionStatus status,
                     LocalDateTime startedAt, LocalDateTime completedAt, String errorMessage, String screenshotPath) {
        this.id = id;
        this.testCaseId = testCaseId;
        this.targetUrl = targetUrl;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
        this.screenshotPath = screenshotPath;
    }

    @PrePersist
    protected void onCreate() {
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ExecutionStatus.PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(Long testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }
}

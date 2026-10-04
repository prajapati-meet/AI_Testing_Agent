package com.aitestagent.bug.entity;

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
@Table(name = "bug_reports")
public class BugReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long executionId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BugSeverity severity;

    @Column(length = 1000)
    private String summary;

    @Column(length = 1000)
    private String expectedResult;

    @Column(length = 1000)
    private String actualResult;

    private String possibleArea;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BugReport() {
    }

    public BugReport(Long id, Long executionId, String title, BugSeverity severity,
                     String summary, String expectedResult, String actualResult,
                     String possibleArea, LocalDateTime createdAt) {
        this.id = id;
        this.executionId = executionId;
        this.title = title;
        this.severity = severity;
        this.summary = summary;
        this.expectedResult = expectedResult;
        this.actualResult = actualResult;
        this.possibleArea = possibleArea;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.severity == null) {
            this.severity = BugSeverity.MEDIUM;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BugSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(BugSeverity severity) {
        this.severity = severity;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getActualResult() {
        return actualResult;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    public String getPossibleArea() {
        return possibleArea;
    }

    public void setPossibleArea(String possibleArea) {
        this.possibleArea = possibleArea;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

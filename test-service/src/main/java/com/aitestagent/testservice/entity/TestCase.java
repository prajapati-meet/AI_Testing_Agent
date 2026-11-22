package com.aitestagent.testservice.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_cases")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String priority;

    @Column(columnDefinition = "TEXT")
    private String expectedResult;

    @Column(name = "user_email")
    private String userEmail;

    @Column(length = 2048)
    private String targetUrl;

    @Column
    private Long explorationId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("stepOrder ASC")
    @JsonManagedReference
    private List<TestStep> steps = new ArrayList<>();

    public TestCase() {
    }

    public TestCase(Long id, String name, String description, String priority, String expectedResult, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.expectedResult = expectedResult;
        this.createdAt = createdAt;
    }

    public TestCase(Long id, String name, String description, String priority, String expectedResult, String targetUrl, Long explorationId, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.expectedResult = expectedResult;
        this.targetUrl = targetUrl;
        this.explorationId = explorationId;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.priority == null) {
            this.priority = "MEDIUM";
        }
    }

    public void addStep(TestStep step) {
        if (step != null) {
            this.steps.add(step);
            step.setTestCase(this);
        }
    }

    public void removeStep(TestStep step) {
        if (step != null) {
            this.steps.remove(step);
            step.setTestCase(null);
        }
    }

    public void setSteps(List<TestStep> steps) {
        this.steps.clear();
        if (steps != null) {
            for (TestStep step : steps) {
                addStep(step);
            }
        }
    }

    // Getters and Setters

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Long getExplorationId() {
        return explorationId;
    }

    public void setExplorationId(Long explorationId) {
        this.explorationId = explorationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<TestStep> getSteps() {
        return steps;
    }
}

package com.aitestagent.testservice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a single deterministic executable step within a TestCase.
 */
@Entity
@Table(name = "test_steps")
public class TestStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    @JsonBackReference
    private TestCase testCase;

    @Column(nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StepAction action;

    @Column(columnDefinition = "TEXT")
    private String target;

    @Column(name = "step_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "step_condition", columnDefinition = "TEXT")
    private String condition;

    public TestStep() {
    }

    public TestStep(int stepOrder, StepAction action, String target, String value, String condition) {
        this.stepOrder = stepOrder;
        this.action = action;
        this.target = target;
        this.value = value;
        this.condition = condition;
    }

    public TestStep(Long id, TestCase testCase, int stepOrder, StepAction action, String target, String value, String condition) {
        this.id = id;
        this.testCase = testCase;
        this.stepOrder = stepOrder;
        this.action = action;
        this.target = target;
        this.value = value;
        this.condition = condition;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public void setTestCase(TestCase testCase) {
        this.testCase = testCase;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public StepAction getAction() {
        return action;
    }

    public void setAction(StepAction action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}

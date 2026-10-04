package com.aitestagent.exploration.entity;

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
@Table(name = "explorations")
public class Exploration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExplorationStatus status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    public Exploration() {
    }

    public Exploration(Long id, String targetUrl, ExplorationStatus status, LocalDateTime startedAt, LocalDateTime completedAt) {
        this.id = id;
        this.targetUrl = targetUrl;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ExplorationStatus.PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public ExplorationStatus getStatus() {
        return status;
    }

    public void setStatus(ExplorationStatus status) {
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
}

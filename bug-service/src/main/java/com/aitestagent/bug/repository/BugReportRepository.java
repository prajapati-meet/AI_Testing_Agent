package com.aitestagent.bug.repository;

import com.aitestagent.bug.entity.BugReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BugReportRepository extends JpaRepository<BugReport, Long> {
    List<BugReport> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    List<BugReport> findAllByOrderByCreatedAtDesc();
}

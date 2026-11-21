package com.aitestagent.testservice.repository;

import com.aitestagent.testservice.entity.TestStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestStepRepository extends JpaRepository<TestStep, Long> {
    List<TestStep> findByTestCaseIdOrderByStepOrderAsc(Long testCaseId);
}

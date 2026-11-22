package com.aitestagent.testservice.repository;

import com.aitestagent.testservice.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    List<TestCase> findAllByOrderByCreatedAtDesc();
}

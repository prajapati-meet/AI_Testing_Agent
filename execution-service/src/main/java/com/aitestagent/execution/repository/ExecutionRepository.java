package com.aitestagent.execution.repository;

import com.aitestagent.execution.entity.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Long> {
    List<Execution> findByUserEmailOrderByStartedAtDesc(String userEmail);
    List<Execution> findAllByOrderByStartedAtDesc();
}

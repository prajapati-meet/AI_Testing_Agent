package com.aitestagent.exploration.repository;

import com.aitestagent.exploration.entity.Exploration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExplorationRepository extends JpaRepository<Exploration, Long> {
    List<Exploration> findByUserEmailOrderByStartedAtDesc(String userEmail);
    List<Exploration> findAllByOrderByStartedAtDesc();
}

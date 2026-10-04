package com.aitestagent.exploration.repository;

import com.aitestagent.exploration.entity.Exploration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExplorationRepository extends JpaRepository<Exploration, Long> {
}

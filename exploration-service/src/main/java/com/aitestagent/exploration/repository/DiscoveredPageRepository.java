package com.aitestagent.exploration.repository;

import com.aitestagent.exploration.entity.DiscoveredPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscoveredPageRepository extends JpaRepository<DiscoveredPage, Long> {

    /**
     * Returns all pages discovered during a specific exploration session,
     * ordered by their database insertion order (id).
     */
    List<DiscoveredPage> findByExplorationIdOrderByIdAsc(Long explorationId);
}

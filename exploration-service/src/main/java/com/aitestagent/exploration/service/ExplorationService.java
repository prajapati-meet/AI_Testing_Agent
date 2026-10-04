package com.aitestagent.exploration.service;

import com.aitestagent.exploration.dto.DiscoveredPageDto;
import com.aitestagent.exploration.dto.ExplorationResponse;
import com.aitestagent.exploration.dto.StartExplorationRequest;
import com.aitestagent.exploration.entity.Exploration;
import com.aitestagent.exploration.entity.ExplorationStatus;
import com.aitestagent.exploration.repository.ExplorationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExplorationService {

    private final ExplorationRepository explorationRepository;

    public ExplorationService(ExplorationRepository explorationRepository) {
        this.explorationRepository = explorationRepository;
    }

    public ExplorationResponse startExploration(StartExplorationRequest request) {
        String targetUrl = (request != null && request.targetUrl() != null)
                ? request.targetUrl()
                : "http://localhost:3000";

        Exploration exploration = new Exploration();
        exploration.setTargetUrl(targetUrl);
        exploration.setStatus(ExplorationStatus.PENDING);
        exploration.setStartedAt(LocalDateTime.now());
        Exploration saved = explorationRepository.save(exploration);

        return new ExplorationResponse(
                saved.getId(),
                saved.getTargetUrl(),
                saved.getStatus(),
                saved.getStartedAt(),
                saved.getCompletedAt(),
                "Exploration session created (placeholder)"
        );
    }

    public ExplorationResponse getExplorationById(Long id) {
        return explorationRepository.findById(id)
                .map(exp -> new ExplorationResponse(
                        exp.getId(),
                        exp.getTargetUrl(),
                        exp.getStatus(),
                        exp.getStartedAt(),
                        exp.getCompletedAt(),
                        "Exploration details retrieved"
                ))
                .orElseGet(() -> new ExplorationResponse(
                        id,
                        "http://localhost:3000",
                        ExplorationStatus.COMPLETED,
                        LocalDateTime.now().minusMinutes(2),
                        LocalDateTime.now(),
                        "Placeholder exploration response"
                ));
    }

    public List<DiscoveredPageDto> getDiscoveredPages(Long id) {
        return List.of(
                new DiscoveredPageDto("http://localhost:3000/", "Home Page", 5, 2, 0, 0),
                new DiscoveredPageDto("http://localhost:3000/login", "Login Page", 2, 1, 1, 2),
                new DiscoveredPageDto("http://localhost:3000/products", "Products Page", 4, 3, 0, 1)
        );
    }
}

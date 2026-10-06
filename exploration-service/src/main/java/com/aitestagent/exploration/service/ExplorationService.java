package com.aitestagent.exploration.service;

import com.aitestagent.exploration.dto.DiscoveredPageDto;
import com.aitestagent.exploration.dto.ExplorationResponse;
import com.aitestagent.exploration.dto.StartExplorationRequest;
import com.aitestagent.exploration.entity.Exploration;
import com.aitestagent.exploration.entity.ExplorationStatus;
import com.aitestagent.exploration.exception.ResourceNotFoundException;
import com.aitestagent.exploration.repository.DiscoveredPageRepository;
import com.aitestagent.exploration.repository.ExplorationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExplorationService {

    private final ExplorationRepository explorationRepository;
    private final DiscoveredPageRepository discoveredPageRepository;
    private final AsyncCrawlService asyncCrawlService;

    public ExplorationService(ExplorationRepository explorationRepository,
                              DiscoveredPageRepository discoveredPageRepository,
                              AsyncCrawlService asyncCrawlService) {
        this.explorationRepository = explorationRepository;
        this.discoveredPageRepository = discoveredPageRepository;
        this.asyncCrawlService = asyncCrawlService;
    }

    /**
     * Creates an exploration record with status PENDING, returns immediately,
     * then fires the actual Playwright crawl asynchronously in the background.
     */
    public ExplorationResponse startExploration(StartExplorationRequest request) {
        String targetUrl = (request != null && request.targetUrl() != null)
                ? request.targetUrl().trim()
                : "http://localhost:3000";

        Exploration exploration = new Exploration();
        exploration.setTargetUrl(targetUrl);
        exploration.setStatus(ExplorationStatus.PENDING);
        exploration.setStartedAt(LocalDateTime.now());
        Exploration saved = explorationRepository.save(exploration);

        // Fire-and-forget: the crawl runs in a dedicated thread pool
        asyncCrawlService.executeCrawl(saved.getId());

        return toResponse(saved, "Exploration started — crawling in progress.");
    }

    /**
     * Returns the current state of an exploration session.
     * Poll this endpoint until status == COMPLETED or FAILED.
     */
    public ExplorationResponse getExplorationById(Long id) {
        Exploration exp = explorationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exploration not found with id: " + id));
        return toResponse(exp, "Exploration details retrieved.");
    }

    /**
     * Returns all pages discovered during a completed exploration.
     * Each DTO contains the full JSON strings for links, buttons, inputs, and forms.
     */
    public List<DiscoveredPageDto> getDiscoveredPages(Long explorationId) {
        // Validate that the exploration exists
        explorationRepository.findById(explorationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exploration not found with id: " + explorationId));

        return discoveredPageRepository
                .findByExplorationIdOrderByIdAsc(explorationId)
                .stream()
                .map(dp -> new DiscoveredPageDto(
                        dp.getId(),
                        explorationId,
                        dp.getPageUrl(),
                        dp.getPageTitle(),
                        dp.getDiscoveredLinks(),
                        dp.getButtons(),
                        dp.getInputs(),
                        dp.getForms(),
                        dp.getNavigationFlow()))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Private helper
    // -------------------------------------------------------------------------

    private ExplorationResponse toResponse(Exploration exp, String message) {
        return new ExplorationResponse(
                exp.getId(),
                exp.getTargetUrl(),
                exp.getStatus(),
                exp.getStartedAt(),
                exp.getCompletedAt(),
                message,
                exp.getNavigationFlowSummary()
        );
    }
}

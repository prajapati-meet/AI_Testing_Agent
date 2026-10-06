package com.aitestagent.exploration.service;

import com.aitestagent.exploration.entity.DiscoveredPage;
import com.aitestagent.exploration.entity.Exploration;
import com.aitestagent.exploration.entity.ExplorationStatus;
import com.aitestagent.exploration.repository.DiscoveredPageRepository;
import com.aitestagent.exploration.repository.ExplorationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles the asynchronous execution of a crawl session.
 *
 * <p>This service is intentionally separated from {@link ExplorationService} so that
 * the {@code @Async} proxy has a chance to intercept the call. If {@code @Async} is
 * placed on a method of the same bean that invokes it, Spring's proxy is bypassed.</p>
 */
@Service
public class AsyncCrawlService {

    private static final Logger log = LoggerFactory.getLogger(AsyncCrawlService.class);

    private final ExplorationRepository explorationRepository;
    private final DiscoveredPageRepository discoveredPageRepository;
    private final PlaywrightCrawlerService crawlerService;

    public AsyncCrawlService(ExplorationRepository explorationRepository,
                             DiscoveredPageRepository discoveredPageRepository,
                             PlaywrightCrawlerService crawlerService) {
        this.explorationRepository = explorationRepository;
        this.discoveredPageRepository = discoveredPageRepository;
        this.crawlerService = crawlerService;
    }

    /**
     * Executes the full crawl pipeline asynchronously.
     *
     * <ol>
     *   <li>Sets exploration status to {@code RUNNING}.</li>
     *   <li>Delegates actual Playwright crawling to {@link PlaywrightCrawlerService}.</li>
     *   <li>Persists each discovered page.</li>
     *   <li>Builds and stores the navigation flow summary on the exploration entity.</li>
     *   <li>Sets status to {@code COMPLETED} or {@code FAILED}.</li>
     * </ol>
     *
     * @param explorationId the ID of the exploration record to run
     */
    @Async("crawlExecutor")
    public void executeCrawl(Long explorationId) {
        Exploration exploration = explorationRepository.findById(explorationId).orElse(null);
        if (exploration == null) {
            log.error("[AsyncCrawl] Exploration {} not found — aborting.", explorationId);
            return;
        }

        try {
            // Mark as RUNNING
            exploration.setStatus(ExplorationStatus.RUNNING);
            explorationRepository.save(exploration);
            log.info("[AsyncCrawl] Exploration {} is now RUNNING for URL: {}",
                    explorationId, exploration.getTargetUrl());

            // Run the Playwright crawl
            List<DiscoveredPage> pages = crawlerService.crawl(exploration);

            // Persist all discovered pages
            discoveredPageRepository.saveAll(pages);
            log.info("[AsyncCrawl] Saved {} discovered pages for exploration {}.",
                    pages.size(), explorationId);

            // Build and persist the navigation flow summary
            String summary = crawlerService.buildNavigationFlowSummary(pages);
            exploration.setNavigationFlowSummary(summary);
            exploration.setStatus(ExplorationStatus.COMPLETED);
            exploration.setCompletedAt(LocalDateTime.now());
            explorationRepository.save(exploration);

            log.info("[AsyncCrawl] Exploration {} COMPLETED successfully.", explorationId);

        } catch (Exception e) {
            log.error("[AsyncCrawl] Exploration {} FAILED: {}", explorationId, e.getMessage(), e);
            try {
                exploration.setStatus(ExplorationStatus.FAILED);
                exploration.setCompletedAt(LocalDateTime.now());
                explorationRepository.save(exploration);
            } catch (Exception saveEx) {
                log.error("[AsyncCrawl] Could not persist FAILED status for exploration {}: {}",
                        explorationId, saveEx.getMessage());
            }
        }
    }
}

package com.aitestagent.exploration;

import com.aitestagent.exploration.dto.DiscoveredPageDto;
import com.aitestagent.exploration.entity.DiscoveredPage;
import com.aitestagent.exploration.entity.Exploration;
import com.aitestagent.exploration.entity.ExplorationStatus;
import com.aitestagent.exploration.repository.DiscoveredPageRepository;
import com.aitestagent.exploration.repository.ExplorationRepository;
import com.aitestagent.exploration.service.ExplorationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExplorationServiceApplicationTests {

    @Autowired
    private ExplorationService explorationService;

    @Autowired
    private ExplorationRepository explorationRepository;

    @Autowired
    private DiscoveredPageRepository discoveredPageRepository;

    @Test
    void contextLoadsAndReturnsPlaceholderPages() {
        Exploration exploration = new Exploration();
        exploration.setTargetUrl("http://localhost:3000");
        exploration.setStatus(ExplorationStatus.COMPLETED);
        exploration.setStartedAt(LocalDateTime.now());
        exploration.setCompletedAt(LocalDateTime.now());
        Exploration savedExploration = explorationRepository.save(exploration);

        DiscoveredPage page = new DiscoveredPage();
        page.setExploration(savedExploration);
        page.setPageUrl("http://localhost:3000/");
        page.setPageTitle("DemoShop Home");
        page.setDiscoveredLinks("[\"/login\"]");
        page.setButtons("[]");
        page.setInputs("[]");
        page.setForms("[]");
        discoveredPageRepository.save(page);

        List<DiscoveredPageDto> pages = explorationService.getDiscoveredPages(savedExploration.getId());
        assertThat(pages).isNotEmpty();
        assertThat(pages.get(0).pageTitle()).isEqualTo("DemoShop Home");
    }
}

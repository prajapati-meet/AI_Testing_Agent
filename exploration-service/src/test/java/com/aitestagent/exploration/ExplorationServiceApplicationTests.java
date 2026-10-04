package com.aitestagent.exploration;

import com.aitestagent.exploration.dto.DiscoveredPageDto;
import com.aitestagent.exploration.service.ExplorationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExplorationServiceApplicationTests {

    @Autowired
    private ExplorationService explorationService;

    @Test
    void contextLoadsAndReturnsPlaceholderPages() {
        List<DiscoveredPageDto> pages = explorationService.getDiscoveredPages(1L);
        assertThat(pages).isNotEmpty();
    }
}

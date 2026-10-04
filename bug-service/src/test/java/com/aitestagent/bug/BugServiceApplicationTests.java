package com.aitestagent.bug;

import com.aitestagent.bug.dto.BugReportResponse;
import com.aitestagent.bug.service.BugReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BugServiceApplicationTests {

    @Autowired
    private BugReportService bugReportService;

    @Test
    void contextLoadsAndReturnsBugReports() {
        List<BugReportResponse> bugReports = bugReportService.getAllBugReports();
        assertThat(bugReports).isNotEmpty();
    }
}

package com.aitestagent.execution;

import com.aitestagent.execution.dto.ExecutionResponse;
import com.aitestagent.execution.service.ExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExecutionServiceApplicationTests {

    @Autowired
    private ExecutionService executionService;

    @Test
    void contextLoadsAndReturnsExecutions() {
        List<ExecutionResponse> executions = executionService.getAllExecutions();
        assertThat(executions).isNotEmpty();
    }
}

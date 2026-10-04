package com.aitestagent.testservice;

import com.aitestagent.testservice.dto.TestCaseResponse;
import com.aitestagent.testservice.service.TestCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TestServiceApplicationTests {

    @Autowired
    private TestCaseService testCaseService;

    @Test
    void contextLoadsAndReturnsTestCases() {
        List<TestCaseResponse> testCases = testCaseService.getAllTestCases();
        assertThat(testCases).isNotEmpty();
    }
}

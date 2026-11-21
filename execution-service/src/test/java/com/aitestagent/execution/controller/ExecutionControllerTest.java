package com.aitestagent.execution.controller;

import com.aitestagent.execution.dto.ExecutionRequest;
import com.aitestagent.execution.dto.ExecutionResponse;
import com.aitestagent.execution.entity.ExecutionStatus;
import com.aitestagent.execution.exception.ResourceNotFoundException;
import com.aitestagent.execution.service.ExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExecutionController.class)
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExecutionService executionService;

    @Test
    @DisplayName("POST /api/executions should trigger execution and return CREATED")
    void shouldTriggerExecution() throws Exception {
        ExecutionRequest request = new ExecutionRequest(1L, "http://localhost:3000");
        ExecutionResponse response = new ExecutionResponse(
                10L, 1L, "http://localhost:3000", ExecutionStatus.PENDING,
                LocalDateTime.now(), null, null, null, null
        );

        given(executionService.triggerExecution(any(ExecutionRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.targetUrl").value("http://localhost:3000"));
    }

    @Test
    @DisplayName("GET /api/executions should return all executions")
    void shouldGetAllExecutions() throws Exception {
        ExecutionResponse response = new ExecutionResponse(
                1L, 1L, "http://localhost:3000", ExecutionStatus.PASSED,
                LocalDateTime.now(), LocalDateTime.now(), null, null, null
        );

        given(executionService.getAllExecutions()).willReturn(List.of(response));

        mockMvc.perform(get("/api/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].status").value("PASSED"));
    }

    @Test
    @DisplayName("GET /api/executions/{id} should return execution detail")
    void shouldGetExecutionById() throws Exception {
        ExecutionResponse response = new ExecutionResponse(
                5L, 2L, "http://localhost:3000", ExecutionStatus.PASSED,
                LocalDateTime.now(), LocalDateTime.now(), null, null, null
        );

        given(executionService.getExecutionById(5L)).willReturn(response);

        mockMvc.perform(get("/api/executions/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.testCaseId").value(2L));
    }

    @Test
    @DisplayName("GET /api/executions/{id} should return 404 when not found")
    void shouldReturn404WhenNotFound() throws Exception {
        given(executionService.getExecutionById(999L))
                .willThrow(new ResourceNotFoundException("Execution not found with id: 999"));

        mockMvc.perform(get("/api/executions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/executions/{id}/screenshot should return image bytes")
    void shouldGetScreenshot() throws Exception {
        byte[] dummyPng = new byte[]{(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        given(executionService.getExecutionScreenshot(5L)).willReturn(dummyPng);

        mockMvc.perform(get("/api/executions/5/screenshot"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(dummyPng));
    }
}

package com.aitestagent.execution.controller;

import com.aitestagent.execution.dto.ExecutionRequest;
import com.aitestagent.execution.dto.ExecutionResponse;
import com.aitestagent.execution.service.ExecutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping
    public ResponseEntity<ExecutionResponse> triggerExecution(
            @RequestBody(required = false) ExecutionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(executionService.triggerExecution(request));
    }

    @GetMapping
    public ResponseEntity<List<ExecutionResponse>> getAllExecutions() {
        return ResponseEntity.ok(executionService.getAllExecutions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExecutionResponse> getExecutionById(@PathVariable Long id) {
        return ResponseEntity.ok(executionService.getExecutionById(id));
    }

    @GetMapping("/demo")
    public ResponseEntity<String> runDemoPoC() {
        try (com.microsoft.playwright.Playwright playwright = com.microsoft.playwright.Playwright.create()) {
            com.microsoft.playwright.Browser browser = playwright.chromium().launch(
                new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true)
            );
            com.microsoft.playwright.Page page = browser.newPage();
            page.navigate("http://localhost:3000");
            
            // Wait a moment for React to render the home page
            page.waitForTimeout(1000);
            
            // Make Playwright actually INTERACT with the page by clicking the Products button
            page.click("text='Browse Products'");
            
            // Wait for the Products page to load
            page.waitForTimeout(1000);
            
            String screenshotName = "demo-screenshot-latest.png";
            page.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions()
                .setPath(java.nio.file.Paths.get(screenshotName))
                .setFullPage(true));
                
            return ResponseEntity.ok("Successfully navigated to target app, clicked 'Browse Products', and saved screenshot as: " + screenshotName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error during Playwright execution: " + e.getMessage());
        }
    }
}

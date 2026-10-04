package com.aitestagent.exploration.controller;

import com.aitestagent.exploration.dto.DiscoveredPageDto;
import com.aitestagent.exploration.dto.ExplorationResponse;
import com.aitestagent.exploration.dto.StartExplorationRequest;
import com.aitestagent.exploration.service.ExplorationService;
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
@RequestMapping("/api/exploration")
public class ExplorationController {

    private final ExplorationService explorationService;

    public ExplorationController(ExplorationService explorationService) {
        this.explorationService = explorationService;
    }

    @PostMapping("/start")
    public ResponseEntity<ExplorationResponse> startExploration(
            @RequestBody(required = false) StartExplorationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(explorationService.startExploration(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExplorationResponse> getExploration(@PathVariable Long id) {
        return ResponseEntity.ok(explorationService.getExplorationById(id));
    }

    @GetMapping("/{id}/pages")
    public ResponseEntity<List<DiscoveredPageDto>> getDiscoveredPages(@PathVariable Long id) {
        return ResponseEntity.ok(explorationService.getDiscoveredPages(id));
    }
}

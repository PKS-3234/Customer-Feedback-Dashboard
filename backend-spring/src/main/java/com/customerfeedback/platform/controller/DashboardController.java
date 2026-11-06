package com.customerfeedback.platform.controller;

import com.customerfeedback.platform.dto.AgentStatusResponse;
import com.customerfeedback.platform.dto.DashboardResponse;
import com.customerfeedback.platform.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboardMetrics() {
        log.info("REST request to get executive dashboard metrics");
        DashboardResponse response = dashboardService.getDashboardMetrics();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/agents")
    public ResponseEntity<List<AgentStatusResponse>> getAgents() {
        log.info("REST request to get agent status list");
        List<AgentStatusResponse> agents = dashboardService.getAgents();
        return ResponseEntity.ok(agents);
    }
}

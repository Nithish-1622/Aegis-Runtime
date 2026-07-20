package com.aegis.runtime.dashboard.controller;

import com.aegis.runtime.dashboard.dto.DashboardSummary;
import com.aegis.runtime.dashboard.service.DashboardService;
import com.aegis.runtime.dashboard.service.DashboardTrendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/runtime/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardTrendService dashboardTrendService;

    public DashboardController(
            DashboardService dashboardService,
            DashboardTrendService dashboardTrendService) {
        this.dashboardService = dashboardService;
        this.dashboardTrendService = dashboardTrendService;
    }

    @GetMapping
    public ResponseEntity<DashboardSummary> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/trends")
    public ResponseEntity<DashboardTrendService.TrendResponse> getTrends(
            @RequestParam(value = "window", required = false) String window) {
        return ResponseEntity.ok(dashboardTrendService.getTrends(window));
    }
}

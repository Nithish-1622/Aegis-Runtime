package com.aegis.runtime.analytics.controller;

import com.aegis.runtime.analytics.dto.RuntimeMetrics;
import com.aegis.runtime.analytics.service.RuntimeMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/runtime")
public class RuntimeMetricsController {

    private final RuntimeMetricsService runtimeMetricsService;

    public RuntimeMetricsController(RuntimeMetricsService runtimeMetricsService) {
        this.runtimeMetricsService = runtimeMetricsService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<RuntimeMetrics> getRuntimeMetrics() {
        return ResponseEntity.ok(runtimeMetricsService.getRuntimeMetrics());
    }
}

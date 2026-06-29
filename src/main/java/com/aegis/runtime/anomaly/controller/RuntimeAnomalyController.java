package com.aegis.runtime.anomaly.controller;

import com.aegis.runtime.anomaly.dto.RuntimeAnomalyResponse;
import com.aegis.runtime.anomaly.service.AnomalyPipelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/runtime")
public class RuntimeAnomalyController {

    private final AnomalyPipelineService anomalyPipelineService;

    public RuntimeAnomalyController(AnomalyPipelineService anomalyPipelineService) {
        this.anomalyPipelineService = anomalyPipelineService;
    }

    @GetMapping("/anomalies")
    public ResponseEntity<RuntimeAnomalyResponse> getAnomalies() {
        return ResponseEntity.ok(anomalyPipelineService.detectAndPersist());
    }
}

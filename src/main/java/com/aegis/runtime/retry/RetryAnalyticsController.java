package com.aegis.runtime.retry;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/runtime")
public class RetryAnalyticsController {

    private final RetryAnalysisService retryAnalysisService;

    public RetryAnalyticsController(RetryAnalysisService retryAnalysisService) {
        this.retryAnalysisService = retryAnalysisService;
    }

    @GetMapping("/retries")
    public ResponseEntity<RetryAnalysis> getRetryAnalysis() {
        return ResponseEntity.ok(retryAnalysisService.analyze());
    }
}

package com.aegis.runtime.dashboard.controller;

import com.aegis.runtime.dashboard.dto.RuntimeHealth;
import com.aegis.runtime.dashboard.service.RuntimeHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/runtime")
public class RuntimeHealthController {

    private final RuntimeHealthService runtimeHealthService;

    public RuntimeHealthController(RuntimeHealthService runtimeHealthService) {
        this.runtimeHealthService = runtimeHealthService;
    }

    @GetMapping("/health")
    public ResponseEntity<RuntimeHealth> getHealth() {
        return ResponseEntity.ok(runtimeHealthService.getHealth());
    }
}

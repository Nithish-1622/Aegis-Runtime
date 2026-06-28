package com.aegis.runtime.analytics.controller;

import com.aegis.runtime.analytics.dto.ServiceLatency;
import com.aegis.runtime.analytics.service.ServiceLatencyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/runtime/services")
public class ServiceLatencyController {

    private final ServiceLatencyService serviceLatencyService;

    public ServiceLatencyController(ServiceLatencyService serviceLatencyService) {
        this.serviceLatencyService = serviceLatencyService;
    }

    @GetMapping("/{serviceName}/latency")
    public ResponseEntity<ServiceLatency> getServiceLatency(@PathVariable String serviceName) {
        ServiceLatency serviceLatency = serviceLatencyService.getMetrics(serviceName);
        if (serviceLatency == null) {
            throw new ResponseStatusException(NOT_FOUND, "Service not found");
        }
        return ResponseEntity.ok(serviceLatency);
    }
}

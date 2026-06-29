package com.aegis.runtime.topology;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/runtime")
public class TopologyController {

    private final DependencyDiscoveryService dependencyDiscoveryService;

    public TopologyController(DependencyDiscoveryService dependencyDiscoveryService) {
        this.dependencyDiscoveryService = dependencyDiscoveryService;
    }

    @GetMapping("/topology")
    public ResponseEntity<ServiceTopology> getTopology() {
        return ResponseEntity.ok(dependencyDiscoveryService.discover());
    }
}

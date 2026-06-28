package com.aegis.runtime.graph.controller;

import com.aegis.runtime.graph.model.ExecutionGraph;
import com.aegis.runtime.graph.service.ExecutionGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/runtime")
public class ExecutionGraphController {

    private final ExecutionGraphService executionGraphService;

    public ExecutionGraphController(ExecutionGraphService executionGraphService) {
        this.executionGraphService = executionGraphService;
    }

    @GetMapping("/graph/{traceId}")
    public ResponseEntity<ExecutionGraph> getGraph(@PathVariable String traceId) {
        ExecutionGraph executionGraph = executionGraphService.buildGraph(traceId);
        if (executionGraph.getNodes().isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Trace not found");
        }
        return ResponseEntity.ok(executionGraph);
    }
}

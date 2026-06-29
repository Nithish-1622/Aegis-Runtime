package com.aegis.runtime.anomaly.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aegis.runtime.anomaly.dto.RuntimeAnomalyResponse;
import com.aegis.runtime.anomaly.service.AnomalyPipelineService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class RuntimeAnomalyControllerTest {

    @Test
    void returnsAnomalyResponse() {
        RuntimeAnomalyController controller = new RuntimeAnomalyController(new AnomalyPipelineServiceStub());

        ResponseEntity<RuntimeAnomalyResponse> response = controller.getAnomalies();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0L, response.getBody().totalAnomalies());
    }

    private static class AnomalyPipelineServiceStub extends AnomalyPipelineService {

        private AnomalyPipelineServiceStub() {
            super(null, null, null);
        }

        @Override
        public RuntimeAnomalyResponse detectAndPersist() {
            return new RuntimeAnomalyResponse(0, 0, 0, 0, 0, List.of());
        }
    }
}

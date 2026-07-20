package com.aegis.runtime.dashboard.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aegis.runtime.dashboard.dto.RuntimeHealth;
import com.aegis.runtime.dashboard.service.RuntimeHealthService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class RuntimeHealthControllerTest {

    @Test
    void returnsHealthStatus() {
        RuntimeHealthController controller = new RuntimeHealthController(new RuntimeHealthServiceStub());

        ResponseEntity<RuntimeHealth> response = controller.getHealth();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("HEALTHY", response.getBody().status());
    }

    private static class RuntimeHealthServiceStub extends RuntimeHealthService {
        private RuntimeHealthServiceStub() {
            super(null, null, null);
        }

        @Override
        public RuntimeHealth getHealth() {
            return new RuntimeHealth("HEALTHY", 100, 0, List.of(), "Overall runtime is healthy.");
        }
    }
}

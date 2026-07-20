package com.aegis.runtime.dashboard.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aegis.runtime.dashboard.dto.DashboardSummary;
import com.aegis.runtime.dashboard.service.DashboardService;
import com.aegis.runtime.dashboard.service.DashboardTrendService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class DashboardControllerTest {

    @Test
    void returnsDashboardSummaryAndHealth() {
        DashboardController controller = new DashboardController(
                new DashboardServiceStub(),
                new DashboardTrendServiceStub());

        ResponseEntity<DashboardSummary> summary = controller.getSummary();

        assertEquals(200, summary.getStatusCode().value());
        assertEquals(0L, summary.getBody().totalRequests());
    }

    @Test
    void returnsTrendData() {
        DashboardController controller = new DashboardController(
                new DashboardServiceStub(),
                new DashboardTrendServiceStub());

        ResponseEntity<DashboardTrendService.TrendResponse> response = controller.getTrends("24h");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("24h", response.getBody().window());
    }

    private static class DashboardServiceStub extends DashboardService {
        private DashboardServiceStub() {
            super(null, null, null, null, null);
        }

        @Override
        public DashboardSummary getSummary() {
            return new DashboardSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    private static class DashboardTrendServiceStub extends DashboardTrendService {
        private DashboardTrendServiceStub() {
            super(null);
        }

        @Override
        public TrendResponse getTrends(String window) {
            return new TrendResponse(window == null ? "1h" : window, List.of());
        }
    }

}

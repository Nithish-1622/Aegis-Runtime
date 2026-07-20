package com.aegis.runtime.dashboard.service;

import com.aegis.runtime.contracts.EventStatus;
import com.aegis.runtime.contracts.EventType;
import com.aegis.runtime.dashboard.dto.DashboardTrend;
import com.aegis.runtime.storage.entity.RuntimeEventEntity;
import com.aegis.runtime.storage.repository.RuntimeEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardTrendService {

    private final RuntimeEventRepository runtimeEventRepository;

    public DashboardTrendService(RuntimeEventRepository runtimeEventRepository) {
        this.runtimeEventRepository = runtimeEventRepository;
    }

    public TrendResponse getTrends(String window) {
        TimeWindow resolvedWindow = TimeWindow.from(window);
        Instant end = Instant.now();
        Instant start = resolvedWindow.start(end);

        List<RuntimeEventEntity> events = runtimeEventRepository.findAll().stream()
                .filter(event -> event.getTimestamp() != null)
                .filter(event -> !event.getTimestamp().isBefore(start) && !event.getTimestamp().isAfter(end))
                .sorted(Comparator.comparing(RuntimeEventEntity::getTimestamp))
                .toList();

        Map<Instant, Bucket> buckets = new HashMap<>();
        for (RuntimeEventEntity event : events) {
            Instant bucketKey = resolvedWindow.bucketStart(event.getTimestamp());
            Bucket bucket = buckets.computeIfAbsent(bucketKey, ignored -> new Bucket());
            bucket.requestCount++;
            if (event.getLatency() != null) {
                bucket.totalLatencyMs += event.getLatency().toMillis();
                bucket.latencyCount++;
            }
            if (event.getStatus() == EventStatus.FAILED || event.getEventType() == EventType.ERROR_OCCURRED) {
                bucket.errorCount++;
            }
            if (event.getEventType() == EventType.RETRY_TRIGGERED) {
                bucket.retryCount++;
            }
        }

        List<DashboardTrend> points = new ArrayList<>();
        buckets.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Bucket bucket = entry.getValue();
                    points.add(new DashboardTrend(
                            entry.getKey(),
                            bucket.requestCount,
                            bucket.latencyCount == 0 ? 0 : (double) bucket.totalLatencyMs / bucket.latencyCount,
                            bucket.errorCount,
                            bucket.retryCount));
                });

        return new TrendResponse(resolvedWindow.apiValue, points);
    }

    public record TrendResponse(String window, List<DashboardTrend> points) {
    }

    private static final class Bucket {
        long requestCount;
        long totalLatencyMs;
        long latencyCount;
        long errorCount;
        long retryCount;
    }

    private enum TimeWindow {
        HOUR("1h", ChronoUnit.HOURS, 1),
        DAY("24h", ChronoUnit.HOURS, 24),
        WEEK("7d", ChronoUnit.DAYS, 7);

        private final String apiValue;
        private final ChronoUnit unit;
        private final int amount;

        TimeWindow(String apiValue, ChronoUnit unit, int amount) {
            this.apiValue = apiValue;
            this.unit = unit;
            this.amount = amount;
        }

        static TimeWindow from(String window) {
            if ("7d".equalsIgnoreCase(window)) {
                return WEEK;
            }
            if ("24h".equalsIgnoreCase(window)) {
                return DAY;
            }
            return HOUR;
        }

        Instant start(Instant end) {
            return switch (this) {
                case HOUR, DAY -> end.minus(amount, unit);
                case WEEK -> end.minus(amount, unit);
            };
        }

        Instant bucketStart(Instant timestamp) {
            ZonedDateTime zoned = timestamp.atZone(ZoneOffset.UTC);
            return switch (this) {
                case HOUR -> zoned.truncatedTo(ChronoUnit.MINUTES).toInstant();
                case DAY -> zoned.truncatedTo(ChronoUnit.HOURS).toInstant();
                case WEEK -> zoned.truncatedTo(ChronoUnit.DAYS).toInstant();
            };
        }
    }
}

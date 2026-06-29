package com.aegis.runtime.retry;

import java.util.List;

public record RetryAnalysis(
        long totalRetries,
        long retryChains,
        double retryAmplificationFactor,
        List<String> servicesWithRetries,
        long longestRetryChain,
        long maxRetriesPerRequest) {
}

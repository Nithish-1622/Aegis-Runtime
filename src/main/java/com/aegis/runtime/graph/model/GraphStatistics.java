package com.aegis.runtime.graph.model;

public class GraphStatistics {

    private final int nodeCount;
    private final int edgeCount;
    private final int requestCount;
    private final int serviceCount;
    private final int retryCount;
    private final int errorCount;
    private final int maxDepth;

    public GraphStatistics(
            int nodeCount,
            int edgeCount,
            int requestCount,
            int serviceCount,
            int retryCount,
            int errorCount,
            int maxDepth) {
        this.nodeCount = nodeCount;
        this.edgeCount = edgeCount;
        this.requestCount = requestCount;
        this.serviceCount = serviceCount;
        this.retryCount = retryCount;
        this.errorCount = errorCount;
        this.maxDepth = maxDepth;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getEdgeCount() {
        return edgeCount;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public int getServiceCount() {
        return serviceCount;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getMaxDepth() {
        return maxDepth;
    }
}

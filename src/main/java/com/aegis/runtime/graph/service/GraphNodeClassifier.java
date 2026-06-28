package com.aegis.runtime.graph.service;

import com.aegis.runtime.contracts.EventType;
import com.aegis.runtime.contracts.RuntimeEvent;
import com.aegis.runtime.graph.model.GraphNodeType;
import org.springframework.stereotype.Component;

@Component
public class GraphNodeClassifier {

    public GraphNodeType classify(RuntimeEvent runtimeEvent) {
        if (runtimeEvent == null || runtimeEvent.eventType() == null) {
            return GraphNodeType.SERVICE;
        }

        EventType eventType = runtimeEvent.eventType();
        return switch (eventType) {
            case REQUEST_STARTED, REQUEST_COMPLETED -> GraphNodeType.REQUEST;
            case SERVICE_CALL_STARTED, SERVICE_CALL_COMPLETED -> GraphNodeType.SERVICE;
            case RETRY_TRIGGERED -> GraphNodeType.RETRY;
            case ERROR_OCCURRED -> GraphNodeType.ERROR;
            case HEARTBEAT -> GraphNodeType.SERVICE;
        };
    }
}

package com.aegis.runtime.contracts;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record Payload(Map<String, Object> attributes) {

    public Payload {
        attributes = attributes == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }
}
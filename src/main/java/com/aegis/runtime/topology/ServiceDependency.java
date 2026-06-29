package com.aegis.runtime.topology;

public record ServiceDependency(String source, String target, long callCount) {
}

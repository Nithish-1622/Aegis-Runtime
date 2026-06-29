package com.aegis.runtime.topology;

import java.util.List;

public record ServiceTopology(List<ServiceNode> services, List<ServiceDependency> dependencies) {
}

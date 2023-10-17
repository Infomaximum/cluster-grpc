package com.infomaximum.cluster.core.service.transport.network.grpc.internal.struct;

import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.core.service.transport.network.LocationRuntimeComponent;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class RNode {

    public final Node node;

    private List<LocationRuntimeComponent> components;

    public RNode(Node node, List<LocationRuntimeComponent> components) {
        this.node = node;
        setComponents(components);
    }

    public List<LocationRuntimeComponent> getComponents() {
        return components;
    }

    public void setComponents(List<LocationRuntimeComponent> components) {
        this.components = Collections.unmodifiableList(components);
    }
}

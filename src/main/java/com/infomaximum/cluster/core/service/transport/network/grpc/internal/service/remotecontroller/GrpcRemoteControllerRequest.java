package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller;

import com.infomaximum.cluster.core.service.transport.network.RemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.RemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.struct.Component;
import com.infomaximum.cluster.utils.GlobalUniqueIdUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class GrpcRemoteControllerRequest implements RemoteControllerRequest {

    private final GrpcNetworkTransitImpl grpcNetworkTransit;

    private final byte currentNode;
    private final Map<Byte, GrpcRemoteControllerRequestItem> items;

    public GrpcRemoteControllerRequest(GrpcNetworkTransitImpl grpcNetworkTransit) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.currentNode = grpcNetworkTransit.getNode();
        this.items = new HashMap<>();
        for (RemoteNode remoteNode : grpcNetworkTransit.targets) {
            if (currentNode == remoteNode.name) {
                continue;
            }
            items.put(remoteNode.name, new GrpcRemoteControllerRequestItem(
                    grpcNetworkTransit, remoteNode.name
            ));
        }
    }

    @Override
    public Object request(Component sourceComponent, int targetComponentUniqueId, String rControllerClassName, Method method, Object[] args) throws Exception {
        byte node = GlobalUniqueIdUtils.getNode(targetComponentUniqueId);
        GrpcRemoteControllerRequestItem controllerRequestItem = items.get(node);
        return controllerRequestItem.transitRequest(sourceComponent, targetComponentUniqueId, rControllerClassName, method, args);
    }
}

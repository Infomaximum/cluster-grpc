package com.infomaximum.cluster.core.service.transport.network.grpc.service.remotecontroller;

import com.infomaximum.cluster.core.service.transport.network.RemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.Node;
import com.infomaximum.cluster.utils.GlobalUniqueIdUtils;

import java.util.HashMap;
import java.util.Map;

public class GrpcRemoteControllerRequest implements RemoteControllerRequest {

    private final GrpcNetworkTransit grpcNetworkTransit;

    private final byte currentNode;
    private final Map<Byte, GrpcRemoteControllerRequestItem> items;

    public GrpcRemoteControllerRequest(GrpcNetworkTransit grpcNetworkTransit) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.currentNode = grpcNetworkTransit.getNode();
        this.items = new HashMap<>();
        for (Node node : grpcNetworkTransit.targets) {
            if (currentNode == node.name) {
                continue;
            }
            items.put(node.name, new GrpcRemoteControllerRequestItem(
                    grpcNetworkTransit, node.name
            ));
        }
    }

    @Override
    public Object request(int targetComponentUniqueId, String rControllerClassName, String methodName, Object[] args) throws Exception {
        byte node = GlobalUniqueIdUtils.getNode(targetComponentUniqueId);
        GrpcRemoteControllerRequestItem controllerRequestItem = items.get(node);
        return controllerRequestItem.transitRequest(targetComponentUniqueId, rControllerClassName, methodName, args);
    }
}

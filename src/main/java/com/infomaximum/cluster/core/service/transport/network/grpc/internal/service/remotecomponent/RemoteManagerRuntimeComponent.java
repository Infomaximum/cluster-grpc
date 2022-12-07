package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecomponent;

import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.core.service.transport.network.grpc.RemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.utils.GlobalUniqueIdUtils;
import com.infomaximum.cluster.utils.RandomUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RemoteManagerRuntimeComponent {

    private final GrpcNetworkTransitImpl grpcNetworkTransit;

    private final byte currentNode;
    private final Map<Byte, RemoteManagerRuntimeComponentItem> items;

    public RemoteManagerRuntimeComponent(GrpcNetworkTransitImpl grpcNetworkTransit) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.currentNode = grpcNetworkTransit.getNode();
        this.items = new HashMap<>();
        for (RemoteNode remoteNode : grpcNetworkTransit.targets) {
            if (currentNode == remoteNode.name) {
                continue;
            }
            items.put(remoteNode.name, new RemoteManagerRuntimeComponentItem(
                    grpcNetworkTransit, remoteNode.name
            ));
        }
    }

    public Collection<RuntimeComponentInfo> getComponents() {
        ArrayList<RuntimeComponentInfo> components = new ArrayList<>();
        for (Map.Entry<Byte, RemoteManagerRuntimeComponentItem> entry : items.entrySet()) {
            components.addAll(entry.getValue().getComponents());
        }
        return components;
    }

    public RuntimeComponentInfo get(int uniqueId) {
        byte node = GlobalUniqueIdUtils.getNode(uniqueId);
        RemoteManagerRuntimeComponentItem remoteManagerRuntimeComponentItem = items.get(node);
        if (remoteManagerRuntimeComponentItem == null) return null;
        return remoteManagerRuntimeComponentItem.get(uniqueId);
    }

    public RuntimeComponentInfo find(String uuid, Class<? extends RController> remoteControllerClazz) {
        ArrayList<RuntimeComponentInfo> contenders = new ArrayList<>();
        for (Map.Entry<Byte, RemoteManagerRuntimeComponentItem> entry : items.entrySet()) {
            RuntimeComponentInfo contender = entry.getValue().find(uuid, remoteControllerClazz);
            if (contender != null) {
                contenders.add(contender);
            }
        }
        if (contenders.isEmpty()) {
            return null;
        } else {
            return contenders.get(RandomUtil.random.nextInt(contenders.size()));
        }
    }

    public Collection<RuntimeComponentInfo> find(Class<? extends RController> remoteControllerClazz) {
        ArrayList<RuntimeComponentInfo> components = new ArrayList<>();
        for (Map.Entry<Byte, RemoteManagerRuntimeComponentItem> entry : items.entrySet()) {
            Collection<RuntimeComponentInfo> items = entry.getValue().find(remoteControllerClazz);
            if (items != null) {
                components.addAll(items);
            }
        }
        return components;
    }
}

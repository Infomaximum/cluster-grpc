package com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.service;

import com.infomaximum.cluster.component.manager.ManagerComponent;
import com.infomaximum.cluster.component.manager.core.ManagerRegisterComponents;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.network.LocationRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channel;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.convert.ConvertProto;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageStartComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageUpdateNode;

import java.util.ArrayList;
import java.util.List;

public class ComponentService {

    private final TransportManager transportManager;

    public ComponentService(TransportManager transportManager) {
        this.transportManager = transportManager;
    }

    public void registerComponents(Channel channel) {
        ManagerRegisterComponents managerComponents = transportManager.cluster
                .getAnyLocalComponent(ManagerComponent.class)
                .getRegisterComponent();
        for (LocationRuntimeComponent locationRuntimeComponent : channel.getRemoteNode().getComponents()) {
            managerComponents.registerRemoteComponent(channel.getRemoteNode().node, locationRuntimeComponent.component());
        }
    }

    public void unRegisterComponents(Channel channel) {
        ManagerRegisterComponents managerComponents = transportManager.cluster
                .getAnyLocalComponent(ManagerComponent.class)
                .getRegisterComponent();
        for (LocationRuntimeComponent locationRuntimeComponent : channel.getRemoteNode().getComponents()) {
            managerComponents.unRegisterRemoteComponent(channel.getRemoteNode().node, locationRuntimeComponent.component());
        }
    }

    public void handleIncomingPacket(Channel channel, PNetPackageUpdateNode value){
        ManagerRegisterComponents registerComponent = transportManager.cluster
                .getAnyLocalComponent(ManagerComponent.class)
                .getRegisterComponent();

        List<LocationRuntimeComponent> newComponents = ConvertProto.convert(channel.getRemoteNode().node.getRuntimeId(), value);
        List<String> uuidOldComponents = channel.getRemoteNode().getComponents().stream()
                .map(locationRuntimeComponent -> locationRuntimeComponent.component().uuid)
                .toList();
        channel.getRemoteNode().setComponents(newComponents);
        for (LocationRuntimeComponent newComponent : newComponents) {
            if (!uuidOldComponents.contains(newComponent.component().uuid)) {
                registerComponent.registerRemoteComponent(channel.getRemoteNode().node, newComponent.component());
            }
        }
    }

    public void handleIncomingPacket(Channel channel, PNetPackageStartComponent value){
        ManagerRegisterComponents managerComponents = transportManager.cluster
                .getAnyLocalComponent(ManagerComponent.class)
                .getRegisterComponent();

        LocationRuntimeComponent locationRuntimeComponent = ConvertProto.convert(channel.getRemoteNode().node.getRuntimeId(), value);
        List<LocationRuntimeComponent> newComponents = new ArrayList<>(channel.getRemoteNode().getComponents());
        newComponents.add(locationRuntimeComponent);
        channel.getRemoteNode().setComponents(newComponents);
        managerComponents.startRemoteComponent(channel.getRemoteNode().node, locationRuntimeComponent.component());
    }
}

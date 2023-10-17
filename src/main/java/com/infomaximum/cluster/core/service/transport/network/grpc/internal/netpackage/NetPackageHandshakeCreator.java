package com.infomaximum.cluster.core.service.transport.network.grpc.internal.netpackage;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.*;
import com.infomaximum.cluster.core.service.transport.network.local.LocalManagerRuntimeComponent;
import com.infomaximum.cluster.struct.Component;

import java.util.stream.Collectors;

public class NetPackageHandshakeCreator {

    //TODO не оптимально
    public static PNetPackage create(GrpcNetworkTransitImpl grpcNetworkTransit) {
        PNetPackageHandshakeNode.Builder nodeBuilder = PNetPackageHandshakeNode.newBuilder()
                .setName(grpcNetworkTransit.getNode().getName())
                .setRuntimeId(grpcNetworkTransit.getNode().getRuntimeId().toString());

        Cluster cluster = grpcNetworkTransit.transportManager.cluster;
        for(Component component: cluster.getLocalComponents()) {
            nodeBuilder.addPNetPackageComponents(buildPackageComponent(component));
        }

        PNetPackageHandshake packageHandshake = PNetPackageHandshake.newBuilder()
                .setNode(nodeBuilder.build())
                .build();

        return PNetPackage.newBuilder().setHandshake(packageHandshake).build();
    }

    public static PNetPackageComponent buildPackageComponent(Component component) {
        return PNetPackageComponent.newBuilder()
                .setUuid(component.getInfo().getUuid())
                .setId(component.getId())
                .addAllClassNameRControllers(
                        component.getTransport().getExecutor().getClassRControllers().stream().map(it -> it.getName()).collect(Collectors.toList())
                )
                .build();
    }

    public static PNetPackage buildPacketUpdateNode(LocalManagerRuntimeComponent localManagerRuntimeComponent) {
        PNetPackageUpdateNode.Builder updateBuilder = PNetPackageUpdateNode.newBuilder();
        for (RuntimeComponentInfo runtimeComponentInfo : localManagerRuntimeComponent.getComponents()) {
            updateBuilder.addPNetPackageComponents(NetPackageHandshakeCreator.buildPackageComponent(runtimeComponentInfo));
        }
        return PNetPackage.newBuilder().setUpdateNode(updateBuilder).build();
    }

    public static PNetPackageComponent buildPackageComponent(RuntimeComponentInfo runtimeComponentInfo) {
        return PNetPackageComponent.newBuilder()
                .setUuid(runtimeComponentInfo.uuid)
                .setId(runtimeComponentInfo.id)
                .addAllClassNameRControllers(runtimeComponentInfo.getClassNameRControllers())
                .build();
    }
}

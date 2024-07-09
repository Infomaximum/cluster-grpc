package com.infomaximum.cluster.core.service.transport.network.grpc.internal.netpackage;

import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.*;
import com.infomaximum.cluster.core.service.transport.network.local.LocalManagerRuntimeComponent;

import java.util.UUID;

public class NetPackageHandshakeCreator {

    //TODO не оптимально
    public static PNetPackage createResponse(GrpcNetworkTransitImpl grpcNetworkTransit) {
        PNetPackageHandshakeNode handshakeNode = buildHandshakeNode(grpcNetworkTransit);
        PNetPackageHandshakeResponse packageHandshake = PNetPackageHandshakeResponse.newBuilder()
                .setNode(handshakeNode)
                .build();
        return PNetPackage.newBuilder().setHandshakeResponse(packageHandshake).build();
    }

    public static PNetPackage createRequest(GrpcNetworkTransitImpl grpcNetworkTransit, UUID channelUuid) {
        PNetPackageHandshakeNode handshakeNode = buildHandshakeNode(grpcNetworkTransit);
        PNetPackageHandshakeRequest packageHandshake = PNetPackageHandshakeRequest.newBuilder()
                .setChannelIdMostSigBits(channelUuid.getMostSignificantBits())
                .setChannelIdLeastSigBit(channelUuid.getLeastSignificantBits())
                .setNode(handshakeNode)
                .build();
        return PNetPackage.newBuilder().setHandshakeRequest(packageHandshake).build();
    }

    private static PNetPackageHandshakeNode buildHandshakeNode(GrpcNetworkTransitImpl grpcNetworkTransit) {
        UUID nodeRuntimeId = grpcNetworkTransit.getNode().getRuntimeId();

        PNetPackageHandshakeNode.Builder nodeBuilder = PNetPackageHandshakeNode.newBuilder()
                .setName(grpcNetworkTransit.getNode().getName())
                .setRuntimeIdMostSigBits(nodeRuntimeId.getMostSignificantBits())
                .setRuntimeIdLeastSigBits(nodeRuntimeId.getLeastSignificantBits());

        LocalManagerRuntimeComponent localManagerRuntimeComponent = grpcNetworkTransit.getManagerRuntimeComponent().getLocalManagerRuntimeComponent();
        for (RuntimeComponentInfo runtimeComponentInfo : localManagerRuntimeComponent.getComponents()) {
            nodeBuilder.addPNetPackageComponents(buildPackageComponent(runtimeComponentInfo));
        }

        return nodeBuilder.build();
    }

    public static PNetPackage buildPacketUpdateNode(LocalManagerRuntimeComponent localManagerRuntimeComponent) {
        PNetPackageUpdateNode.Builder updateBuilder = PNetPackageUpdateNode.newBuilder();
        for (RuntimeComponentInfo runtimeComponentInfo : localManagerRuntimeComponent.getComponents()) {
            updateBuilder.addPNetPackageComponents(buildPackageComponent(runtimeComponentInfo));
        }
        return PNetPackage.newBuilder().setUpdateNode(updateBuilder).build();
    }

    public static PNetPackageComponent buildPackageComponent(RuntimeComponentInfo runtimeComponentInfo) {
        PNetPackageComponent.Builder builder = PNetPackageComponent.newBuilder()
                .setUuid(runtimeComponentInfo.uuid)
                .setId(runtimeComponentInfo.id)
                .addAllClassNameRControllers(runtimeComponentInfo.getClassNameRControllers());
        if (runtimeComponentInfo.version != null) {
            builder.setVersion(runtimeComponentInfo.version.toString());
        }
        return builder.build();
    }
}

package com.infomaximum.cluster.core.service.transport.network.grpc.internal.netpackage;

import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcRemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.*;
import com.infomaximum.cluster.core.service.transport.network.local.LocalManagerRuntimeComponent;

import java.util.UUID;

public class NetPackageHandshakeCreator {

    //TODO не оптимально
    public static PNetPackage createResponse(GrpcNetworkTransitImpl grpcNetworkTransit) {
        long declaredTimeoutMillis = grpcNetworkTransit.getWaitResponseTimeout().toMillis();
        PNetPackageHandshakeNode handshakeNode = buildHandshakeNode(grpcNetworkTransit, declaredTimeoutMillis);
        PNetPackageHandshakeResponse packageHandshake = PNetPackageHandshakeResponse.newBuilder()
                .setNode(handshakeNode)
                .build();
        return PNetPackage.newBuilder().setHandshakeResponse(packageHandshake).build();
    }

    public static PNetPackage createRequest(GrpcNetworkTransitImpl grpcNetworkTransit, UUID channelUuid, GrpcRemoteNode target) {
        long declaredTimeoutMillis = target.resolveEffectiveWaitResponseTimeout(
                grpcNetworkTransit.getWaitResponseTimeout()).toMillis();
        PNetPackageHandshakeNode handshakeNode = buildHandshakeNode(grpcNetworkTransit, declaredTimeoutMillis);
        PNetPackageHandshakeRequest packageHandshake = PNetPackageHandshakeRequest.newBuilder()
                .setChannelIdMostSigBits(channelUuid.getMostSignificantBits())
                .setChannelIdLeastSigBit(channelUuid.getLeastSignificantBits())
                .setNode(handshakeNode)
                .build();
        return PNetPackage.newBuilder().setHandshakeRequest(packageHandshake).build();
    }

    private static PNetPackageHandshakeNode buildHandshakeNode(GrpcNetworkTransitImpl grpcNetworkTransit, long declaredTimeoutMillis) {
        UUID nodeRuntimeId = grpcNetworkTransit.getNode().getRuntimeId();

        // protobuf-поле имеет тип int32; реалистичные timeout-значения (секунды–часы) с запасом
        // умещаются, но при ошибочной конфигурации (> ~24.8 дней) клампим, чтобы избежать молчаливого
        // усечения отрицательным значением
        int declaredTimeoutMillisInt = declaredTimeoutMillis > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) declaredTimeoutMillis;

        PNetPackageHandshakeNode.Builder nodeBuilder = PNetPackageHandshakeNode.newBuilder()
                .setName(grpcNetworkTransit.getNode().getName())
                .setRuntimeIdMostSigBits(nodeRuntimeId.getMostSignificantBits())
                .setRuntimeIdLeastSigBits(nodeRuntimeId.getLeastSignificantBits())
                .setProtocolVersion(grpcNetworkTransit.getProtocolVersion())
                .setWaitResponseTimeoutMillis(declaredTimeoutMillisInt);

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

    public static PNetPackage buildPackageStartComponent(RuntimeComponentInfo runtimeComponentInfo) {
        PNetPackageComponent pNetPackageComponent = buildPackageComponent(runtimeComponentInfo);
        PNetPackageStartComponent.Builder builder = PNetPackageStartComponent.newBuilder()
                .setPNetPackageComponent(pNetPackageComponent);
        return PNetPackage.newBuilder().setStartComponent(builder).build();
    }
}

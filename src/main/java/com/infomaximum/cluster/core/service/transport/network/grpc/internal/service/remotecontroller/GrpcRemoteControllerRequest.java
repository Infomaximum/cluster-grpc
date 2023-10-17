package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller;

import com.google.protobuf.ByteString;
import com.infomaximum.cluster.core.service.transport.executor.ComponentExecutorTransport;
import com.infomaximum.cluster.core.service.transport.network.RemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcRemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channel;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.ChannelImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channels;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.*;
import com.infomaximum.cluster.struct.Component;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GrpcRemoteControllerRequest implements RemoteControllerRequest {

    private final GrpcNetworkTransitImpl grpcNetworkTransit;

    private final AtomicInteger ids;
    private final ConcurrentHashMap<Integer, CompletableFuture<PNetPackageResponse>> requests;

    public GrpcRemoteControllerRequest(GrpcNetworkTransitImpl grpcNetworkTransit) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.ids = new AtomicInteger();
        this.requests = new ConcurrentHashMap<>();
    }

    private int nextPackageId() {
        return ids.updateAndGet(value -> (value == Integer.MAX_VALUE) ? 1 : value + 1);
    }

    @Override
    public ComponentExecutorTransport.Result request(Component sourceComponent, UUID targetNodeRuntimeId, int targetComponentId, String rControllerClassName, int methodKey, byte[][] args) throws Exception {
        ChannelImpl channel = (ChannelImpl) grpcNetworkTransit.getChannels().getChannel(targetNodeRuntimeId);
        if (channel == null) {
            throw grpcNetworkTransit.transportManager.getExceptionBuilder().buildRemoteComponentUnavailableException(targetNodeRuntimeId, targetComponentId, rControllerClassName, methodKey, null);
        }

        int packageId = nextPackageId();

        CompletableFuture<PNetPackageResponse> completableFuture = new CompletableFuture<>();
        requests.put(packageId, completableFuture);

        //Формируем пакет-запрос и его отправляем
        PNetPackageRequest.Builder builderPackageRequest = PNetPackageRequest.newBuilder()
                .setPackageId(packageId)
                .setTargetComponentId(targetComponentId)
                .setRControllerClassName(rControllerClassName)
                .setMethodKey(methodKey);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                builderPackageRequest.addArgs(ByteString.copyFrom(args[i]));
            }
        }
        PNetPackage netPackage = PNetPackage.newBuilder().setRequest(builderPackageRequest).build();
        channel.sent(netPackage);

        PNetPackageResponse netPackageResponse = completableFuture.get();

        if (!netPackageResponse.getException().isEmpty()) {
            return new ComponentExecutorTransport.Result(null, netPackageResponse.getException().toByteArray());
        } else {
            return new ComponentExecutorTransport.Result(netPackageResponse.getResult().toByteArray(), null);
        }
    }

    public void handleIncomingPacket(PNetPackageResponse response) {
        CompletableFuture<PNetPackageResponse> completableFuture = requests.remove(response.getPackageId());
        completableFuture.complete(response);
    }

    public void handleIncomingPacket(PNetPackageRequest request, StreamObserver<PNetPackage> responseObserver) {
        byte[][] byteArgs = new byte[request.getArgsCount()][];
        for (int i = 0; i < byteArgs.length; i++) {
            byteArgs[i] = request.getArgs(i).toByteArray();
        }
        ComponentExecutorTransport.Result result = grpcNetworkTransit.transportManager.localRequest(
                request.getTargetComponentId(),
                request.getRControllerClassName(),
                request.getMethodKey(),
                byteArgs
        );

        PNetPackageResponse.Builder responseBuilder = PNetPackageResponse.newBuilder()
                .setPackageId(request.getPackageId());
        if (result.exception() != null) {
            responseBuilder.setException(ByteString.copyFrom(result.exception()));
        } else {
            responseBuilder.setResult(ByteString.copyFrom(result.value()));
        }
        responseObserver.onNext(PNetPackage.newBuilder().setResponse(responseBuilder).build());
    }
}

package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller;

import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channel;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageResponse;

import java.util.concurrent.CompletableFuture;

public record NetRequest(Channel channel, int componentId, String rControllerClassName, int methodKey, CompletableFuture<PNetPackageResponse> completableFuture) {
}

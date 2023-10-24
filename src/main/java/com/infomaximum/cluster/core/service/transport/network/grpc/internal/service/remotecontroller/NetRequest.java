package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller;

import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channel;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageResponse;

import java.util.concurrent.CompletableFuture;

public record NetRequest(Channel channel, int componentId, String rControllerClassName, int methodKey, Timeout timeout, CompletableFuture<PNetPackageResponse> completableFuture) {
}

class Timeout {

    public long timeFail;

    public Timeout(long timeFail) {
        this.timeFail = timeFail;
    }

}
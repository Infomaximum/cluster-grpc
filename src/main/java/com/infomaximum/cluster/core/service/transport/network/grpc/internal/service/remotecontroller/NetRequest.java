package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller;

import com.infomaximum.cluster.core.service.transport.executor.ComponentExecutorTransport;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record NetRequest(UUID targetNodeRuntimeId,
                         int componentId,
                         String rControllerClassName,
                         int methodKey,
                         Timeout timeout,
                         CompletableFuture<ComponentExecutorTransport.Result> completableFuture) {
}

class Timeout {

    public final long requestTimeoutMillis;

    public volatile long timeFail;

    public Timeout(long requestTimeoutMillis, long timeFail) {
        this.requestTimeoutMillis = requestTimeoutMillis;
        this.timeFail = timeFail;
    }

}

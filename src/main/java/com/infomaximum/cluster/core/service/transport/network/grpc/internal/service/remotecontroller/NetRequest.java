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

    public long timeFail;

    public Timeout(long timeFail) {
        this.timeFail = timeFail;
    }

}
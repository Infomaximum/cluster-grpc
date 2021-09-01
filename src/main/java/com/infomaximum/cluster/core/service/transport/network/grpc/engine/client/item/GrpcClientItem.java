package com.infomaximum.cluster.core.service.transport.network.grpc.engine.client.item;

import com.infomaximum.cluster.core.service.transport.network.grpc.struct.Node;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

public class GrpcClientItem implements AutoCloseable {

    public final Node node;

    public final ManagedChannel channel;

    public GrpcClientItem(Node node) {
        this.node = node;

        channel = ManagedChannelBuilder.forTarget(node.target)
                .usePlaintext()
                .disableRetry()
                .build();
    }


    @Override
    public void close() {
        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
    }
}

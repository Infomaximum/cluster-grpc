package com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine.client.item;

import com.infomaximum.cluster.core.service.transport.network.grpc.RemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.exception.ClusterGrpcException;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GrpcClientItem implements AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(GrpcClientItem.class);

    private final GrpcNetworkTransitImpl grpcNetworkTransit;
    public final RemoteNode remoteNode;
    public final ManagedChannel channel;

    public GrpcClientItem(GrpcNetworkTransitImpl grpcNetworkTransit, RemoteNode remoteNode, byte[] fileCertChain, byte[] filePrivateKey, TrustManagerFactory trustStore) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.remoteNode = remoteNode;

        if (filePrivateKey == null) {
            channel = NettyChannelBuilder.forTarget(remoteNode.target)
                    .usePlaintext()
                    .disableRetry()
                    .build();
        } else {
            SslContext sslContext;
            try {
                sslContext = GrpcSslContexts.forClient().trustManager(trustStore)
                        .keyManager(new ByteArrayInputStream(fileCertChain), new ByteArrayInputStream(filePrivateKey))
                        .build();

            } catch (IOException e) {
                throw new ClusterGrpcException(e);
            }
            channel = NettyChannelBuilder.forTarget(remoteNode.target)
                    .sslContext(sslContext)
                    .disableRetry()
                    .build();
        }
        channel.notifyWhenStateChanged(ConnectivityState.READY, () -> {
            grpcNetworkTransit.fireEventOnConnect(grpcNetworkTransit.getNode(), remoteNode.name);
        });
    }


    @Override
    public void close() {
        grpcNetworkTransit.fireEventOnDisconnect(grpcNetworkTransit.getNode(), remoteNode.name);
        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
    }
}

package com.infomaximum.cluster.core.service.transport.network.grpc.engine.client.item;

import com.infomaximum.cluster.core.service.transport.network.grpc.RemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.exception.ClusterGrpcException;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GrpcClientItem implements AutoCloseable {

    public final RemoteNode remoteNode;

    public final ManagedChannel channel;

    public GrpcClientItem(RemoteNode remoteNode, byte[] fileCertChain, byte[] filePrivateKey, TrustManagerFactory trustStore) {
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
    }


    @Override
    public void close() {
        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
    }
}

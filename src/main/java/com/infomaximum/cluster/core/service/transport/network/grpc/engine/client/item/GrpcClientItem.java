package com.infomaximum.cluster.core.service.transport.network.grpc.engine.client.item;

import com.infomaximum.cluster.core.service.transport.network.grpc.struct.Node;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;

import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

public class GrpcClientItem implements AutoCloseable {

    public final Node node;

    public final ManagedChannel channel;

    public GrpcClientItem(Node node) {
        this.node = node;

        if (node.certificate == null) {
            channel = NettyChannelBuilder.forTarget(node.target)
                    .usePlaintext()
                    .disableRetry()
                    .build();
        } else {
            SslContext sslContext;
            try {
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                keyStore.load(null, new char[0]);
                keyStore.setCertificateEntry("", node.certificate);

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);

                sslContext = GrpcSslContexts.forClient().trustManager(tmf).build();
            } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException e) {
                throw new RuntimeException(e);
            }
            channel = NettyChannelBuilder.forTarget(node.target)
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

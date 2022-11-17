package com.infomaximum.cluster.core.service.transport.network.grpc.engine.server;

import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.grpc.exception.ClusterGrpcException;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceRemoteControllerRequestImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceRemoteManagerComponentGrpcImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.utils.CertificateUtils;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;


import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class GrpcServer implements AutoCloseable {

    public final GrpcNetworkTransit grpcNetworkTransit;

    private final int port;

    private final byte[] fileCertChain;
    private final byte[] filePrivateKey;
    private final TrustManagerFactory trustStore;

    private Server server;

    private PServiceRemoteManagerComponentGrpcImpl serviceRemoteManagerComponent;

    public GrpcServer(GrpcNetworkTransit grpcNetworkTransit, int port, byte[] fileCertChain, byte[] filePrivateKey, TrustManagerFactory trustStore) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.port = port;

        this.fileCertChain = fileCertChain;
        this.filePrivateKey = filePrivateKey;
        this.trustStore = trustStore;

        start();
    }

    private void start() {
        this.serviceRemoteManagerComponent = new PServiceRemoteManagerComponentGrpcImpl(this);

        ServerBuilder serverBuilder;
        if (filePrivateKey != null) {
            SslContext sslContext;
            try {
                sslContext = GrpcSslContexts
                        .forServer(new ByteArrayInputStream(fileCertChain), new ByteArrayInputStream(filePrivateKey))
                        .trustManager(trustStore)//Необходимо передавать клиенские сертификаты для валидации
//                        .trustManager()//Попытаться найти способ передачи отозвонных сертефикатов- в крайнем случае можно обойтись
                        .clientAuth(ClientAuth.REQUIRE)
                        .build();
            } catch  (IOException e) {
                throw new ClusterGrpcException(e);
            }
            serverBuilder = NettyServerBuilder.forPort(port);
            ((NettyServerBuilder) serverBuilder).sslContext(sslContext);
        } else {
            serverBuilder = ServerBuilder.forPort(port);
        }


        serverBuilder
                .addService(serviceRemoteManagerComponent)
                .addService(new PServiceRemoteControllerRequestImpl(this));
        try {
            server = serverBuilder.build().start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        serviceRemoteManagerComponent.close();
        server.shutdown();
        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
        }
    }
}

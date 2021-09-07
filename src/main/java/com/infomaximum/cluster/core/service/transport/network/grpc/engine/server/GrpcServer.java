package com.infomaximum.cluster.core.service.transport.network.grpc.engine.server;

import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceRemoteControllerRequestImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceRemoteManagerComponentGrpcImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;


import java.io.ByteArrayInputStream;
import java.io.IOException;

public class GrpcServer implements AutoCloseable {

    public final GrpcNetworkTransit grpcNetworkTransit;

    private final int port;

    private final byte[] serverCertChain;
    private final byte[] serverPrivateKey;

    private Server server;

    private PServiceRemoteManagerComponentGrpcImpl serviceRemoteManagerComponent;

    public GrpcServer(GrpcNetworkTransit grpcNetworkTransit, int port, byte[] serverCertChain, byte[] serverPrivateKey) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.port = port;

        this.serverCertChain = serverCertChain;
        this.serverPrivateKey = serverPrivateKey;

        start();
    }

    private void start() {
        this.serviceRemoteManagerComponent = new PServiceRemoteManagerComponentGrpcImpl(this);

        ServerBuilder serverBuilder = ServerBuilder.forPort(port);
        if (serverPrivateKey != null) {
            serverBuilder.useTransportSecurity(new ByteArrayInputStream(serverCertChain), new ByteArrayInputStream(serverPrivateKey));
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

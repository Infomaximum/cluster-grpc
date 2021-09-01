package com.infomaximum.cluster.core.service.transport.network.grpc.engine.server;

import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceRemoteControllerRequestImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceRemoteManagerComponentGrpcImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;


import java.io.IOException;

public class GrpcServer implements AutoCloseable {

    public final GrpcNetworkTransit grpcNetworkTransit;

    private final int port;

    private Server server;

    private PServiceRemoteManagerComponentGrpcImpl serviceRemoteManagerComponent;

    public GrpcServer(GrpcNetworkTransit grpcNetworkTransit, int port) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.port = port;
        start();
    }

    private void start() {
        this.serviceRemoteManagerComponent = new PServiceRemoteManagerComponentGrpcImpl(this);

        try {
            server = ServerBuilder.forPort(port)
                    .addService(serviceRemoteManagerComponent)
                    .addService(new PServiceRemoteControllerRequestImpl(this))
                    .build().start();
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

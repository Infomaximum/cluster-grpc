package com.infomaximum.cluster.core.service.transport.network.grpc;

import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.network.ManagerRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.NetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.RemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.engine.client.GrpcClient;
import com.infomaximum.cluster.core.service.transport.network.grpc.engine.server.GrpcServer;
import com.infomaximum.cluster.core.service.transport.network.grpc.service.remotecontroller.GrpcRemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.Node;
import com.infomaximum.cluster.core.service.transport.network.local.LocalNetworkTransit;
import com.infomaximum.cluster.core.service.transport.struct.NetworkTransitState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GrpcNetworkTransit extends NetworkTransit {

    private final static Logger log = LoggerFactory.getLogger(GrpcNetworkTransit.class);

    public final TransportManager transportManager;

    private final byte nameName;
    private final int port;
    public final List<Node> targets;

    private final byte[] serverCertChain;
    private final byte[] serverPrivateKey;

    private final GrpcServer grpcServer;
    public final GrpcClient grpcClient;
    private final ManagerRuntimeComponent managerRuntimeComponent;
    private final RemoteControllerRequest remoteControllerRequest;

    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    private GrpcNetworkTransit(GrpcNetworkTransit.Builder builder, TransportManager transportManager, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.transportManager = transportManager;
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;

        this.nameName = builder.nodeName;
        this.port = builder.port;
        this.targets = builder.targets;

        this.serverCertChain = builder.serverCertChain;
        this.serverPrivateKey = builder.serverPrivateKey;

        this.grpcClient = new GrpcClient(targets);
        this.managerRuntimeComponent = new GrpcManagerRuntimeComponent(this);
        this.grpcServer = new GrpcServer(this, port, serverCertChain, serverPrivateKey);

        this.remoteControllerRequest = new GrpcRemoteControllerRequest(this);

        setState(NetworkTransitState.STARTED);
    }

    @Override
    public byte getNode() {
        return nameName;
    }

    @Override
    public ManagerRuntimeComponent getManagerRuntimeComponent() {
        return managerRuntimeComponent;
    }

    @Override
    public RemoteControllerRequest getRemoteControllerRequest() {
        return remoteControllerRequest;
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler;
    }

    @Override
    public void close() {
        grpcClient.close();
        grpcServer.close();
    }

    public static class Builder extends NetworkTransit.Builder {

        private final byte nodeName;
        private final int port;

        private byte[] serverCertChain;
        private byte[] serverPrivateKey;

        private final List<Node> targets;

        private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

        public Builder(byte nodeName, int port, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
            this.nodeName = nodeName;
            this.port = port;
            this.targets = new ArrayList<>();
            this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        }

        public Builder addTarget(Node target) {
            if (target.name != nodeName) {
                this.targets.add(target);
            }
            return this;
        }

        public Builder withTransportSecurity(byte[] serverCertChain, byte[] serverPrivateKey) {
            if (serverCertChain == null) {
                throw new IllegalArgumentException();
            }
            if (serverPrivateKey == null) {
                throw new IllegalArgumentException();
            }
            this.serverCertChain = serverCertChain;
            this.serverPrivateKey = serverPrivateKey;
            return this;
        }

        public GrpcNetworkTransit build(TransportManager transportManager) {
            return new GrpcNetworkTransit(this, transportManager, uncaughtExceptionHandler);
        }

    }

}

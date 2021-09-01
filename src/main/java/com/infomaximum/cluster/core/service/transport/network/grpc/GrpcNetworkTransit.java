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

import java.util.ArrayList;
import java.util.List;

public class GrpcNetworkTransit extends NetworkTransit {

    private final static Logger log = LoggerFactory.getLogger(GrpcNetworkTransit.class);

    public final TransportManager transportManager;

    private final byte nameName;
    private final int port;
    public final List<Node> targets;

    private final GrpcServer grpcServer;
    public final GrpcClient grpcClient;
    private final ManagerRuntimeComponent managerRuntimeComponent;
    private final RemoteControllerRequest remoteControllerRequest;

    private GrpcNetworkTransit(TransportManager transportManager, byte nameName, int port, List<Node> targets) {
        this.transportManager = transportManager;

        this.nameName = nameName;
        this.port = port;
        this.targets = targets;

        this.grpcClient = new GrpcClient(targets);
        this.managerRuntimeComponent = new GrpcManagerRuntimeComponent(this);
        this.grpcServer = new GrpcServer(this, port);

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

    @Override
    public void close() {
        grpcClient.close();
        grpcServer.close();
    }

    public static class Builder extends NetworkTransit.Builder {

        private final byte nameName;
        private final int port;

        private final List<Node> targets;

        public Builder(byte nameName, int port) {
            this.nameName = nameName;
            this.port = port;
            this.targets = new ArrayList<>();
        }

        public Builder addTarget(Node target) {
            this.targets.add(target);
            return this;
        }

        public GrpcNetworkTransit build(TransportManager transportManager) {
            return new GrpcNetworkTransit(transportManager, nameName, port, targets);
        }

    }

}

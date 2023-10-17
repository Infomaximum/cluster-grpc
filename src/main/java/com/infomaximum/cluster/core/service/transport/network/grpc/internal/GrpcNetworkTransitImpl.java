package com.infomaximum.cluster.core.service.transport.network.grpc.internal;

import com.infomaximum.cluster.NetworkTransit;
import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.network.ManagerRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.RemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcRemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channels;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine.GrpcManagerRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine.server.GrpcServer;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.notification.NotificationUpdateComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller.GrpcRemoteControllerRequest;
import com.infomaximum.cluster.core.remote.packer.RemotePackerObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManagerFactory;
import java.util.List;
import java.util.Random;

public class GrpcNetworkTransitImpl implements NetworkTransit {

    private final static Logger log = LoggerFactory.getLogger(GrpcNetworkTransit.class);

    public final TransportManager transportManager;

    private final GrpcNode node;

    public final RemotePackerObject remotePackerObject;

    public final List<GrpcRemoteNode> targets;
    private final GrpcServer grpcServer;
    private final ManagerRuntimeComponent managerRuntimeComponent;
    private final RemoteControllerRequest remoteControllerRequest;

    private final Channels channels;

    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;


    public GrpcNetworkTransitImpl(GrpcNetworkTransit.Builder builder, TransportManager transportManager, byte[] fileCertChain, byte[] filePrivateKey, TrustManagerFactory trustStore, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.transportManager = transportManager;
        this.remotePackerObject = transportManager.getRemotePackerObject();
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;

        node = new GrpcNode.Builder(builder.port).withName(builder.nodeName).build();

        this.targets = builder.getTargets();

        this.remoteControllerRequest = new GrpcRemoteControllerRequest(this);
        this.channels = new Channels.Builder(this)
                .withClient(targets, fileCertChain, filePrivateKey, trustStore)
                .build();

        this.managerRuntimeComponent = new GrpcManagerRuntimeComponent(this, channels);
        new NotificationUpdateComponent(node, managerRuntimeComponent.getLocalManagerRuntimeComponent(), channels);

        this.grpcServer = new GrpcServer(this, channels, builder.port, fileCertChain, filePrivateKey, trustStore);
    }

    @Override
    public Node getNode() {
        return node;
    }

    public Channels getChannels() {
        return channels;
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
    public List<Node> getRemoteNodes() {
        return channels.getRemoteNodes();
    }

    @Override
    public void start() {
        this.channels.start();
        this.grpcServer.start();
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler;
    }


    @Override
    public void close() {
        channels.close();
        grpcServer.close();
    }
}

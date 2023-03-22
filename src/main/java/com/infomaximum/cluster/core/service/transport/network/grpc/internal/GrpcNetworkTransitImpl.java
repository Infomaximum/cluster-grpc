package com.infomaximum.cluster.core.service.transport.network.grpc.internal;

import com.infomaximum.cluster.NetworkTransit;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.network.ManagerRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.RemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.grpc.RemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.UpdateConnect;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine.GrpcManagerRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine.client.GrpcClient;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine.server.GrpcServer;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller.GrpcRemoteControllerRequest;
import com.infomaximum.cluster.core.remote.packer.RemotePackerObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManagerFactory;
import java.util.List;

public class GrpcNetworkTransitImpl implements NetworkTransit {

    private final static Logger log = LoggerFactory.getLogger(GrpcNetworkTransit.class);

    public final TransportManager transportManager;

    public final RemotePackerObject remotePackerObject;

    public final List<RemoteNode> targets;
    public final GrpcClient grpcClient;
    private final byte nodeName;
    private final int port;
    private final GrpcServer grpcServer;
    private final ManagerRuntimeComponent managerRuntimeComponent;
    private final RemoteControllerRequest remoteControllerRequest;

    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    private final List<UpdateConnect> listeners;

    public GrpcNetworkTransitImpl(GrpcNetworkTransit.Builder builder, TransportManager transportManager, byte[] fileCertChain, byte[] filePrivateKey, TrustManagerFactory trustStore, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.transportManager = transportManager;
        this.remotePackerObject = transportManager.getRemotePackerObject();
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;

        this.nodeName = builder.nodeName;
        this.port = builder.port;
        this.targets = builder.getTargets();
        this.listeners = builder.getUpdateConnectListeners();

        this.grpcClient = new GrpcClient(this, fileCertChain, filePrivateKey, trustStore);
        this.managerRuntimeComponent = new GrpcManagerRuntimeComponent(this);

        this.grpcServer = new GrpcServer(this, port, fileCertChain, filePrivateKey, trustStore);

        this.remoteControllerRequest = new GrpcRemoteControllerRequest(this);
    }

    @Override
    public byte getNode() {
        return nodeName;
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

    public void fireEventOnConnect(byte source, byte target){
        for (UpdateConnect listener: listeners) {
            listener.onConnect(source, target);
        }
    }

    public void fireEventOnDisconnect(byte source, byte target){
        for (UpdateConnect listener: listeners) {
            listener.onDisconnect(source, target);
        }
    }

    @Override
    public void close() {
        grpcClient.close();
        grpcServer.close();
    }
}

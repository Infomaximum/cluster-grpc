package com.infomaximum.cluster.core.service.transport.network.grpc.internal.pservice;

import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.ChannelImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.ChannelServer;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channels;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine.GrpcPoolExecutor;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.netpackage.NetPackageHandshakeCreator;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller.GrpcRemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.PackageLog;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceExchangeGrpc;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageHandshakeRequest;
import com.infomaximum.cluster.event.CauseNodeDisconnect;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PServiceExchangeImpl extends PServiceExchangeGrpc.PServiceExchangeImplBase {

    private final static Logger log = LoggerFactory.getLogger(PServiceExchangeImpl.class);

    private final GrpcRemoteControllerRequest remoteControllerRequest;
    private final TransportManager transportManager;
    private final Channels channels;
    private final GrpcPoolExecutor grpcPoolExecutor;

    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private volatile ChannelServer serverChannel;

    public PServiceExchangeImpl(Channels channels) {
        GrpcNetworkTransitImpl grpcNetworkTransit = (GrpcNetworkTransitImpl) channels.transportManager.networkTransit;
        this.remoteControllerRequest = (GrpcRemoteControllerRequest) grpcNetworkTransit.getRemoteControllerRequest();
        this.transportManager = channels.transportManager;
        this.channels = channels;
        this.uncaughtExceptionHandler = channels.getUncaughtExceptionHandler();
        this.grpcPoolExecutor = grpcNetworkTransit.grpcPoolExecutor;
    }


    @Override
    public StreamObserver<PNetPackage> exchange(StreamObserver<PNetPackage> responseObserver) {
        serverChannel = null;

        StreamObserver<PNetPackage> requestObserver = new StreamObserver<PNetPackage>() {

            @Override
            public void onNext(PNetPackage requestPackage) {
                try {
                    if (serverChannel != null && log.isTraceEnabled()) {
                        log.trace("Incoming packet: {} to channel: {}", PackageLog.toString(requestPackage), serverChannel);
                    }

                    if (serverChannel == null) {
                        serverChannel = initChannel(responseObserver, requestPackage);
                    } else {
                        grpcPoolExecutor.execute(() -> {
                            try {
                                handleIncomingPacket(requestPackage);
                            } catch (Throwable t) {
                                uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t);
                            }
                        });
                    }
                } catch (Throwable t) {
                    uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                CauseNodeDisconnect.Type typeCause = CauseNodeDisconnect.Type.EXCEPTION;
                try {
                    destroyChannel(new CauseNodeDisconnect(typeCause, throwable));
                    log.error("onError", throwable);
                } catch (Throwable t) {
                    uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t);
                }
            }

            @Override
            public void onCompleted() {
                try {
                    destroyChannel(CauseNodeDisconnect.NORMAL);
                    log.error("onCompleted");
                } catch (Throwable t) {
                    uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t);
                }
            }
        };
        return requestObserver;
    }

    private ChannelServer initChannel(StreamObserver<PNetPackage> responseObserver, PNetPackage requestPackage) {
        if (!requestPackage.hasHandshakeRequest()) {
            log.error("Unknown state, channel: null, packet: {}. Disconnect", requestPackage.toString());
            //TODO надо переподнимать соединение, а не падать
            uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), new RuntimeException("Unknown state"));
            return null;
        }

        //Сообщаем о себе и регистрируем канал
        PNetPackageHandshakeRequest handshakeRequest = requestPackage.getHandshakeRequest();

        PNetPackage handshakeResponse = NetPackageHandshakeCreator.createResponse((GrpcNetworkTransitImpl) transportManager.networkTransit);
        responseObserver.onNext(handshakeResponse);

        ChannelServer channelServer = new ChannelServer.Builder(responseObserver, handshakeRequest).build();

        Node currentNode = transportManager.cluster.node;
        Node channelRemoteNode = channelServer.remoteNode.node;
        if (currentNode.getRuntimeId().equals(channelRemoteNode.getRuntimeId())) {
            log.error("Loop connect: ignore channel");
            responseObserver.onCompleted();
            return null;
        }

        channels.registerChannel(channelServer);
        log.trace("Incoming packet: {} to channel: {}", PackageLog.toString(requestPackage), channelServer);
        return channelServer;
    }


    private void handleIncomingPacket(PNetPackage requestPackage) {
        ChannelImpl channel = serverChannel;
        if (channel == null) { // Канал может быть закрыт в другом потоке
            return;
        }
        if (requestPackage.hasRequest()) {
            remoteControllerRequest.handleIncomingPacket(requestPackage.getRequest(), channel);
        } else if (requestPackage.hasResponse()) {
            remoteControllerRequest.handleIncomingPacket(requestPackage.getResponse());
        } else if (requestPackage.hasResponseProcessing()) {
            remoteControllerRequest.handleIncomingPacket(requestPackage.getResponseProcessing());
        }  else if (requestPackage.hasBody()) {
            remoteControllerRequest.handleIncomingPacket(requestPackage.getBody());
        } else if (requestPackage.hasUpdateNode()) {
            channel.handleIncomingPacket(requestPackage.getUpdateNode());
        } else if (requestPackage.hasPing()) {
            channels.getPingPongService().handleIncomingPing(channel, requestPackage.getPing());
        } else if (requestPackage.hasPong()) {
            channels.getPingPongService().handleIncomingPong(channel, requestPackage.getPong());
        } else {
            log.error("Unknown state, channel: {}, packet: {}. Disconnect", channel, requestPackage.toString());
            //TODO надо переподнимать соединение, а не падать
            uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), new RuntimeException("Unknown state"));
        }
    }

    private void destroyChannel(CauseNodeDisconnect cause) {
        if (serverChannel != null) {
            channels.unRegisterChannel(serverChannel, cause);
            serverChannel = null;
        }
    }
}

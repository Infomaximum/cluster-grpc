package com.infomaximum.cluster.core.service.transport.network.grpc.internal.pservice;

import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.ChannelImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.ChannelServer;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channels;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine.server.GrpcServer;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.netpackage.NetPackageHandshakeCreator;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller.GrpcRemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceExchangeGrpc;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PServiceExchangeImpl extends PServiceExchangeGrpc.PServiceExchangeImplBase {

    private final static Logger log = LoggerFactory.getLogger(PServiceExchangeImpl.class);

    private final GrpcRemoteControllerRequest remoteControllerRequest;
    private final TransportManager transportManager;
    private final Channels channels;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public PServiceExchangeImpl(GrpcServer grpcServer, Channels channels) {
        this.remoteControllerRequest = (GrpcRemoteControllerRequest) grpcServer.grpcNetworkTransit.getRemoteControllerRequest();
        this.transportManager = grpcServer.grpcNetworkTransit.transportManager;
        this.channels = channels;
        this.uncaughtExceptionHandler = grpcServer.grpcNetworkTransit.getUncaughtExceptionHandler();
    }


    @Override
    public StreamObserver<PNetPackage> exchange(StreamObserver<PNetPackage> responseObserver) {
        final ChannelImpl[] serverChannel = {null};

        StreamObserver<PNetPackage> requestObserver = new StreamObserver<PNetPackage>() {

            @Override
            public void onNext(PNetPackage requestPackage) {
                try {
                    if (serverChannel[0] == null && requestPackage.hasHandshake()) {
                        //Сообщаем о себе и регистрируем канал
                        PNetPackage answerHandshake = NetPackageHandshakeCreator.create((GrpcNetworkTransitImpl) transportManager.networkTransit);
                        responseObserver.onNext(answerHandshake);

                        serverChannel[0] = new ChannelServer.Builder(responseObserver, requestPackage.getHandshake()).build();

                        Node currentNode = transportManager.cluster.node;
                        Node channelRemoteNode = serverChannel[0].remoteNode.node;
                        if (currentNode.getRuntimeId().equals(channelRemoteNode.getRuntimeId())) {
                            log.error("Loop connect: ignore channel");
                            responseObserver.onCompleted();
                            return;
                        }

                        channels.registerChannel(serverChannel[0]);
                    } else if (serverChannel[0] !=null && requestPackage.hasRequest()) {
                        remoteControllerRequest.handleIncomingPacket(requestPackage.getRequest(), responseObserver);
                    } else if (serverChannel[0] !=null && requestPackage.hasResponse()) {
                        remoteControllerRequest.handleIncomingPacket(requestPackage.getResponse());
                    } else if (serverChannel[0] !=null && requestPackage.hasResponseProcessing()) {
                        remoteControllerRequest.handleIncomingPacket(requestPackage.getResponseProcessing());
                    } else if (serverChannel[0] !=null && requestPackage.hasUpdateNode()) {
                        serverChannel[0].handleIncomingPacket(requestPackage.getUpdateNode());
                    } else {
                        log.error("Unknown state, channel: {}, packet: {}. Disconnect", serverChannel[0], requestPackage.toString());
                        //TODO надо переподнимать соединение, а не падать
                        uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), new RuntimeException("TODO: need reconnect"));
                    }
                } catch (Throwable t) {
                    uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                try {
                    destroyChannel(serverChannel);
                    log.error("onError", throwable);
                } catch (Throwable t) {
                    uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t);
                }
            }

            @Override
            public void onCompleted() {
                try {
                    destroyChannel(serverChannel);
                    log.error("onCompleted");
                } catch (Throwable t) {
                    uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t);
                }
            }
        };
        return requestObserver;
    }

    private void destroyChannel(ChannelImpl[] serverChannel) {
        if (serverChannel[0] != null) {
            channels.unRegisterChannel(serverChannel[0]);
            serverChannel[0] = null;
        }
    }
}

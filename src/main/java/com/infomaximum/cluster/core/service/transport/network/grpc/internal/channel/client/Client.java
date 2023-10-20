package com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.client;

import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcRemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.exception.ClusterGrpcException;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channel;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.ChannelImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channels;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.netpackage.NetPackageHandshakeCreator;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller.GrpcRemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.MLogger;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceExchangeGrpc;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.*;
import com.infomaximum.cluster.utils.ExecutorUtil;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Client implements AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(Client.class);

    private final static int TIMEOUT_REPEAT_CONNECT = 1000;//Пауза между попытками подключения(в милисекундах)
    public final GrpcRemoteNode remoteNode;
    public final ManagedChannel channel;
    private final GrpcNetworkTransitImpl grpcNetworkTransit;
    private final GrpcRemoteControllerRequest remoteControllerRequest;
    private final Channels channels;
    private final PServiceExchangeGrpc.PServiceExchangeStub exchangeStub;
    private final StreamObserver<PNetPackage> responseObserver;
    private final MLogger mLog;

    private volatile ChannelImpl clientChannel;
    private volatile StreamObserver<PNetPackage> requestObserver;

    private volatile boolean isClosed = false;

    public Client(GrpcNetworkTransitImpl grpcNetworkTransit, Channels channels, GrpcRemoteNode remoteNode, byte[] fileCertChain, byte[] filePrivateKey, TrustManagerFactory trustStore) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.remoteControllerRequest = (GrpcRemoteControllerRequest) grpcNetworkTransit.getRemoteControllerRequest();
        this.channels = channels;
        this.remoteNode = remoteNode;

        if (filePrivateKey == null) {
            channel = NettyChannelBuilder.forTarget(remoteNode.target)
                    .usePlaintext()
                    .disableRetry()
                    .build();
        } else {
            SslContext sslContext;
            try {
                sslContext = GrpcSslContexts.forClient().trustManager(trustStore)
                        .keyManager(new ByteArrayInputStream(fileCertChain), new ByteArrayInputStream(filePrivateKey))
                        .build();

            } catch (IOException e) {
                throw new ClusterGrpcException(e);
            }
            channel = NettyChannelBuilder.forTarget(remoteNode.target)
                    .sslContext(sslContext)
                    .disableRetry()
                    .build();
        }

        this.mLog = new MLogger(log, 60 * 1000 / TIMEOUT_REPEAT_CONNECT);//Раз в 1 минуту

        exchangeStub = PServiceExchangeGrpc.newStub(channel);
        responseObserver =
                new StreamObserver<PNetPackage>() {
                    @Override
                    public void onNext(PNetPackage netPackage) {
                        try {
                            if (clientChannel == null && netPackage.hasHandshake()) {
                                clientChannel = (ChannelImpl) new Channel.Builder(requestObserver, netPackage.getHandshake()).build();

                                //Проверяем, что не подключились к себе же
                                Node currentNode = grpcNetworkTransit.getNode();
                                Node channelRemoteNode = clientChannel.remoteNode.node;
                                if (currentNode.getRuntimeId().equals(channelRemoteNode.getRuntimeId())) {
                                    log.error("Loop connect, disconnect: {},", remoteNode.target);
                                    isClosed = true;
                                    channel.shutdownNow();
                                    return;
                                }

                                channels.registerChannel(clientChannel);

                                //За то время пока устанавливалось соединение могли загрузится новые компоненты - стоит повторно отправить свое состояние
                                PNetPackage netPackageUpdateNode = NetPackageHandshakeCreator.buildPacketUpdateNode(grpcNetworkTransit.getManagerRuntimeComponent().getLocalManagerRuntimeComponent());
                                requestObserver.onNext(netPackageUpdateNode);
                            } else if (clientChannel != null && netPackage.hasRequest()) {//Пришел запрос
                                remoteControllerRequest.handleIncomingPacket(netPackage.getRequest(), requestObserver);
                            } else if (clientChannel != null && netPackage.hasResponse()) {//Пришел ответ
                                remoteControllerRequest.handleIncomingPacket(netPackage.getResponse());
                            } else if (clientChannel != null && netPackage.hasUpdateNode()) {
                                clientChannel.handleIncomingPacket(netPackage.getUpdateNode());
                            } else {
                                log.error("Unknown state, channel: {}, packet: {}. Disconnect", clientChannel, netPackage.toString());
                                //TODO надо переподнимать соединение, а не падать
                                grpcNetworkTransit.getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new RuntimeException("TODO: need reconnect"));
                            }
                        } catch (Throwable t) {
                            grpcNetworkTransit.getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        mLog.warn("Error connect to remote node: {}, exception: {}", remoteNode.target, t);
                        destroyChannel();

                        ExecutorUtil.executors.execute(() -> {
                            try {
                                Thread.sleep(TIMEOUT_REPEAT_CONNECT);
                            } catch (InterruptedException e) {

                            }
                            reconnect();
                        });
                    }

                    @Override
                    public void onCompleted() {
                        log.warn("Completed connection with remote node: {}, repeat...", remoteNode.target);
                        destroyChannel();
                        ExecutorUtil.executors.execute(() -> {
                            try {
                                Thread.sleep(TIMEOUT_REPEAT_CONNECT);
                            } catch (InterruptedException e) {

                            }
                            reconnect();
                        });
                    }
                };
    }

    private void reconnect() {
        //Отзываем невалидный канал
        destroyChannel();

        if (isClosed) return;

        //Инициализируем
        requestObserver = exchangeStub.exchange(responseObserver);
        PNetPackage packageHandshake = NetPackageHandshakeCreator.create(grpcNetworkTransit);
        requestObserver.onNext(packageHandshake);
    }

    private void destroyChannel() {
        if (clientChannel != null) {
            channels.unRegisterChannel(clientChannel);
            clientChannel = null;
        }
    }

    public void start() {
        reconnect();
    }

    @Override
    public void close() {
        isClosed = true;
        try {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
    }

}

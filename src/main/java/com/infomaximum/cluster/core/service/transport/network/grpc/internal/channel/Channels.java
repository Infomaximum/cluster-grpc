package com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel;

import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.network.LocationRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcRemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.client.Clients;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.server.GrpcServer;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.service.PingPongService;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller.GrpcRemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.struct.RNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.ExecutorUtil;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.PackageLog;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit.Builder.Server;
import com.infomaximum.cluster.event.CauseNodeDisconnect;
import com.infomaximum.cluster.event.UpdateNodeConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManagerFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Channels implements AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(Channels.class);

    private final static int DEFAULT_ATTEMPT = 50;

    public final TransportManager transportManager;
    private final ChannelList channelList;

    private final Clients clients;
    private final GrpcServer grpcServer;

    private final PingPongService pingPongService;

    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    private Channels(Builder builder) {
        this.transportManager = builder.grpcNetworkTransit.transportManager;
        this.uncaughtExceptionHandler = builder.grpcNetworkTransit.getUncaughtExceptionHandler();
        this.channelList = new ChannelList(this, (GrpcRemoteControllerRequest) builder.grpcNetworkTransit.getRemoteControllerRequest());
        this.clients = new Clients(
                builder.grpcNetworkTransit, channelList,
                builder.targets, builder.clientFileCertChain, builder.clientFilePrivateKey, builder.clientTrustStore
        );
        if (builder.server != null) {
            this.grpcServer = new GrpcServer(this, builder.server.port(), builder.clientFileCertChain, builder.clientFilePrivateKey, builder.clientTrustStore);
        } else {
            this.grpcServer = null;
        }
        this.pingPongService = new PingPongService(channelList, builder.pingPongInterval, builder.pingPongTimeout);
    }


    public Channel getChannel(UUID nodeRuntimeId) {
        return channelList.getRandomChannel(nodeRuntimeId);
    }

    public Channel getChannel(UUID nodeRuntimeId, int attempt) {
        return channelList.getRandomChannel(nodeRuntimeId, attempt);
    }

    public void sendPacketWithRepeat(UUID targetNodeRuntimeId, PNetPackage netPackage) throws Exception {
        sendPacket(targetNodeRuntimeId, netPackage, DEFAULT_ATTEMPT);
    }

    public void sendPacket(UUID targetNodeRuntimeId, PNetPackage netPackage, int attempt) throws Exception {
        ChannelImpl channel;
        while (true) {
            channel = (ChannelImpl) getChannel(targetNodeRuntimeId, attempt);
            if (channel == null) {
                throw transportManager.getExceptionBuilder().buildRemoteComponentUnavailableException(targetNodeRuntimeId, null);
            }

            try {
                channel.send(netPackage);
                return;
            } catch (Exception e) {
                //Пробуем найти другой канал
                log.debug("Error send packed: {}, find another chanel", PackageLog.toString(netPackage), e);
            }
        }
    }

    public void fireEventConnectNode(Node node) {
        ExecutorUtil.executorsVirtualThreads.execute(() -> {
            for (UpdateNodeConnect updateNodeConnect : transportManager.updateNodeConnectListeners) {
                try {
                    updateNodeConnect.onConnect(node);
                } catch (Exception e) {
                    uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
                }
            }
        });
    }

    public void fireEventDisconnectNode(Node node, CauseNodeDisconnect cause) {
        ExecutorUtil.executorsVirtualThreads.execute(() -> {
            for (UpdateNodeConnect updateNodeConnect : transportManager.updateNodeConnectListeners) {
                try {
                    updateNodeConnect.onDisconnect(node, cause);
                } catch (Exception e) {
                    uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
                }
            }
        });
    }

    //TODO Переписать на итераторы
    public ArrayList<LocationRuntimeComponent> getComponents() {
        ArrayList<LocationRuntimeComponent> list = new ArrayList<LocationRuntimeComponent>();
        for (UUID nodeRutimeId : channelList.getNodes()) {
            Channel channel = channelList.getRandomChannel(nodeRutimeId);
            if (channel == null) continue;

            RNode remoteNode = channel.getRemoteNode();
            list.addAll(remoteNode.getComponents());
        }
        return list;
    }

    public void registerChannel(Channel channel) {
        channelList.addChannel(channel);
    }

    public void unRegisterChannel(Channel channel, CauseNodeDisconnect cause) {
        channelList.removeChannel(channel, cause);
    }

    public List<Node> getRemoteNodes() {
        return channelList.getRemoteNodes() ;
    }

    public void sendBroadcast(PNetPackage netPackage) {
        channelList.sendBroadcast(netPackage);
    }

    public void start() {
        if (grpcServer != null) {
            grpcServer.start();
        }
        clients.start();
    }

    public PingPongService getPingPongService() {
        return pingPongService;
    }

    public Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return transportManager.cluster.getUncaughtExceptionHandler();
    }

    @Override
    public void close() {
        pingPongService.close();
        if (grpcServer != null) {
            grpcServer.close();
        }
        clients.close();
    }


    public static class Builder {

        private final GrpcNetworkTransitImpl grpcNetworkTransit;
        private byte[] clientFileCertChain;
        private byte[] clientFilePrivateKey;
        private TrustManagerFactory clientTrustStore;

        private Server server;
        private List<GrpcRemoteNode> targets;

        private Duration pingPongInterval;
        private Duration pingPongTimeout;

        public Builder(GrpcNetworkTransitImpl grpcNetworkTransit) {
            this.grpcNetworkTransit = grpcNetworkTransit;
        }

        public Builder withSsl(byte[] fileCertChain, byte[] filePrivateKey, TrustManagerFactory trustStore) {
            this.clientFileCertChain = fileCertChain;
            this.clientFilePrivateKey = filePrivateKey;
            this.clientTrustStore = trustStore;
            return this;
        }

        public Builder withServer(Server server) {
            this.server = server;
            return this;
        }

        public Builder withTargets(List<GrpcRemoteNode> targets) {
            this.targets = targets;
            return this;
        }

        public Builder withPingPongTimeout(Duration interval, Duration timeout) {
            this.pingPongInterval = interval;
            this.pingPongTimeout = timeout;
            return this;
        }

        public Channels build() {
            return new Channels(this);
        }
    }
}

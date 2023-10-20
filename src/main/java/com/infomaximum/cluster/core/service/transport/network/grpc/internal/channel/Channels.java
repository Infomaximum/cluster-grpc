package com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel;

import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.UpdateNodeConnect;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.network.LocationRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcRemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.client.Clients;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller.GrpcRemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.struct.RNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;

import javax.net.ssl.TrustManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Channels implements AutoCloseable {

    public final TransportManager transportManager;
    private final ChannelList channelList;

    private final Clients clients;

    private Channels(Builder builder) {
        this.transportManager = builder.grpcNetworkTransit.transportManager;
        this.channelList = new ChannelList(this, (GrpcRemoteControllerRequest) builder.grpcNetworkTransit.getRemoteControllerRequest());
        this.clients = new Clients(
                builder.grpcNetworkTransit, channelList,
                builder.clientTargets, builder.clientFileCertChain, builder.clientFilePrivateKey, builder.clientTrustStore
        );
    }
    public Channel getChannel(UUID nodeRutimeId) {
        return channelList.getRandomChannel(nodeRutimeId);
    }

    protected void fireEventConnectNode(Node node) {
        for(UpdateNodeConnect updateNodeConnect: transportManager.updateNodeConnectListeners) {
            updateNodeConnect.onConnect(node);
        }
    }

    public void fireEventDisconnectNode(Node node) {
        for(UpdateNodeConnect updateNodeConnect: transportManager.updateNodeConnectListeners) {
            updateNodeConnect.onDisconnect(node);
        }
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

    public void unRegisterChannel(Channel channel) {
        channelList.removeChannel(channel);
    }

    public List<Node> getRemoteNodes() {
        return channelList.getRemoteNodes() ;
    }

    public void sendBroadcast(PNetPackage netPackage) {
        channelList.sendBroadcast(netPackage);
    }

    public void start() {
        clients.start();
    }

    @Override
    public void close() {
        clients.close();
    }


    public static class Builder {

        private final GrpcNetworkTransitImpl grpcNetworkTransit;

        private List<GrpcRemoteNode> clientTargets;
        private byte[] clientFileCertChain;
        private byte[] clientFilePrivateKey;
        private TrustManagerFactory clientTrustStore;


        public Builder(GrpcNetworkTransitImpl grpcNetworkTransit) {
            this.grpcNetworkTransit = grpcNetworkTransit;
        }

        public Builder withClient(List<GrpcRemoteNode> targets, byte[] fileCertChain, byte[] filePrivateKey, TrustManagerFactory trustStore) {
            this.clientTargets = targets;
            this.clientFileCertChain = fileCertChain;
            this.clientFilePrivateKey = filePrivateKey;
            this.clientTrustStore = trustStore;
            return this;
        }

        public Channels build() {
            return new Channels(this);
        }
    }
}

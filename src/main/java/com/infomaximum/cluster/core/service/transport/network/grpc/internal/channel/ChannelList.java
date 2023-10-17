package com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel;

import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.client.Client;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.RandomUtils;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChannelList {

    private final static Logger log = LoggerFactory.getLogger(ChannelList.class);

    public final Channels channels;
    private final Map<UUID, List<Channel>> channelItems;

    public ChannelList(Channels channels) {
        this.channels = channels;
        this.channelItems = new HashMap<>();
    }

    public void addChannel(Channel channel) {
        Node remoteNode = channel.getRemoteNode().node;
        UUID remoteNodeRuntimeId = remoteNode.getRuntimeId();
        List<Channel> items = channelItems.get(remoteNodeRuntimeId);
        if (items == null) {
            synchronized (channelItems) {
                items = channelItems.get(remoteNodeRuntimeId);
                if (items == null) {
                    items = new ArrayList<>();
                    channelItems.put(remoteNodeRuntimeId, items);
                }
            }
        }

        boolean fireEvent = false;
        synchronized (items) {
            items.add(channel);

            if (items.size() == 1) {
                fireEvent = true;
            }
        }

        //Отправляем оповещение
        if (fireEvent) {
            channels.fireEventConnectNode(remoteNode);
        }
    }

    public void removeChannel(Channel channel) {
        Node remoteNode = channel.getRemoteNode().node;

        UUID remoteNodeRuntimeId = remoteNode.getRuntimeId();
        List<Channel> items = channelItems.get(remoteNodeRuntimeId);
        if (items == null) return;

        boolean fireEvent = false;
        synchronized (items) {
            items.remove(channel);

            if (items.size() == 0) {
                fireEvent = true;
            }
        }

        //Отправляем оповещение
        if (fireEvent) {
            channels.fireEventDisconnectNode(remoteNode);
        }
    }

    public Channel getRandomChannel(UUID nodeRutimeId) {
        List<Channel> items = channelItems.get(nodeRutimeId);
        if (items == null) return null;
        synchronized (items) {
            int size = items.size();
            if (size == 0) return null;
            return items.get(RandomUtils.random.nextInt(size));
        }
    }

    public Set<UUID> getNodes() {
        return channelItems.keySet();
    }

    public List<Node> getRemoteNodes() {
        List<Node> nodes = new ArrayList<>();
        for (List<Channel> items : channelItems.values()) {
            synchronized (items) {
                if (items.size() > 0) {
                    nodes.add(items.get(0).getRemoteNode().node);
                }
            }
        }
        return nodes;
    }

    public void sendBroadcast(PNetPackage netPackage) {
        for (List<Channel> iChannels : channelItems.values()) {
            for (Channel channel : iChannels) {
                if (channel.isAvailable()) {
                    ChannelImpl channelImpl = (ChannelImpl) channel;
                    try {
                        channelImpl.sent(netPackage);
                    } catch (Exception e) {
                        log.error("Error send broadcast", e);
                    }
                }
            }
        }

    }
}

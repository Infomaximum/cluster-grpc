package com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel;

import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.utils.ChannelIterator;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller.GrpcRemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.LogUtils;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.event.CauseNodeDisconnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChannelList {

    private final static Logger log = LoggerFactory.getLogger(ChannelList.class);

    public final Channels channels;
    private final GrpcRemoteControllerRequest remoteControllerRequest;
    private final Map<UUID, List<Channel>> channelItems;

    public ChannelList(Channels channels, GrpcRemoteControllerRequest remoteControllerRequest) {
        this.channels = channels;
        this.remoteControllerRequest = remoteControllerRequest;
        this.channelItems = new ConcurrentHashMap<>();
    }

    public void addChannel(Channel channel) {
        Node remoteNode = channel.getRemoteNode().node;
        UUID remoteNodeRuntimeId = remoteNode.getRuntimeId();
        List<Channel> items = channelItems.putIfAbsent(remoteNodeRuntimeId, new CopyOnWriteArrayList<>());
        if (items == null) {
            items = channelItems.get(remoteNodeRuntimeId);
        }

        boolean fireEvent = false;
        synchronized (items) {
            items.add(channel);

            if (items.size() == 1) {
                fireEvent = true;
            }
        }

        log.debug("Add channel: {}, total: {}", channel, items.size());

        //Отправляем оповещение
        if (fireEvent) {
            channels.fireEventConnectNode(remoteNode);
        }
    }

    public void removeChannel(Channel channel, CauseNodeDisconnect cause) {
        ((ChannelImpl) channel).destroy();

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

        log.debug("Remove channel: {}, total: {}", channel, items.size());

        //Отправляем оповещение
        if (fireEvent) {
            channels.fireEventDisconnectNode(remoteNode, cause);
        }
    }

    public Channel getRandomChannel(UUID nodeRuntimeId, int attempt) {
        Channel channel = getRandomChannel(nodeRuntimeId);
        if (channel != null) {
            return channel;
        } else if (attempt > 0) {
            log.debug("Attempt find channel to node: {}, last: {}", nodeRuntimeId, attempt);
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                return null;
            }
            return getRandomChannel(nodeRuntimeId, attempt - 1);
        } else {
            log.debug("Not found channel to node: {}, stackTrace: {}", nodeRuntimeId, LogUtils.toStringStackTrace(Thread.currentThread()));
            return null;
        }
    }

    public Channel getRandomChannel(UUID nodeRuntimeId) {
        List<Channel> items = channelItems.get(nodeRuntimeId);
        if (items == null) return null;
        synchronized (items) {
            int size = items.size();
            if (size == 0) return null;

            int index = new Random().nextInt(size);
            Channel result = null;
            int i = 0;
            for (Channel channel : items) {
                if (!channel.isAvailable()) continue;
                result = channel;
                if (i++ == index) break;
            }
            return result;
        }
    }

    public Set<UUID> getNodes() {
        HashSet<UUID> nodes = new HashSet<>();
        for (Map.Entry<UUID, List<Channel>> entry : channelItems.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                nodes.add(entry.getKey());
            }
        }
        return nodes;
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

    public ChannelIterator getChannelIterator() {
        return new ChannelIterator(channelItems);
    }

    /**
     * Принудительно разрываем соединение с каналом
     * @param channel
     */
    public void killChannel(Channel channel, CauseNodeDisconnect cause) {
        ChannelImpl channelImpl = (ChannelImpl) channel;
        channelImpl.kill(cause.throwable);
        removeChannel(channel, cause);
    }

    public void sendBroadcast(PNetPackage netPackage) {
        for (List<Channel> iChannels : channelItems.values()) {
            for (Channel channel : iChannels) {
                if (!channel.isAvailable()) continue;

                ChannelImpl channelImpl = (ChannelImpl) channel;
                try {
                    channelImpl.send(netPackage);
                } catch (Exception e) {
                    log.error("Error send broadcast", e);
                }
            }
        }
    }
}

package com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel;

import com.infomaximum.cluster.core.service.transport.network.grpc.internal.struct.RNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;

import java.util.UUID;

public interface Channel {

    UUID getUuid();

    RNode getRemoteNode();

    boolean isAvailable();

    ChannelType getType();

    long getChannelTimeoutMillis();

    void send(PNetPackage value);

}

package com.infomaximum.cluster.core.service.transport.network.grpc;

public interface UpdateConnect {

    void onConnect(byte source, byte target);

    void onDisconnect(byte source, byte target);

}

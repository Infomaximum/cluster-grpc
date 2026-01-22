package com.infomaximum.cluster.core.service.transport.network.grpc;

import com.infomaximum.cluster.Node;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class GrpcNode implements Node {

    private final String name;
    private final UUID runtimeId;
    private final boolean isLocal;

    private GrpcNode(String name, UUID runtimeId, boolean isLocal) {
        this.name = name;
        this.runtimeId = runtimeId;
        this.isLocal = isLocal;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public UUID getRuntimeId() {
        return runtimeId;
    }

    @Override
    public boolean isLocal() {
        return isLocal;
    }

    @Override
    public String toString() {
        return "GrpcNode{" +
                "name='" + name + '\'' +
                ", runtimeId=" + runtimeId +
                ", isLocal=" + isLocal +
                '}';
    }

    public static class Builder {

        private String name;
        private UUID runtimeId;
        private final boolean isLocal;

        public Builder(GrpcNetworkTransit.Builder.Server server, boolean isLocal) {
            try {
                this.name = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                this.name = "unknownhost";
            }
            if (server != null) {
                this.name += ":" + server.port();
            }

            this.runtimeId = UUID.randomUUID();
            this.isLocal = isLocal;
        }

        public Builder(String name, UUID runtimeId, boolean isLocal) {
            this.name = name;
            this.runtimeId = runtimeId;
            this.isLocal = isLocal;
        }

        public Builder withName(String name){
            this.name = name;
            return this;
        }

        public GrpcNode build(){
            return new GrpcNode(name, runtimeId, isLocal);
        }
    }
}

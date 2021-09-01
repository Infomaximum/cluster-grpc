package com.infomaximum.cluster.core.service.transport.network.grpc.struct;

public class Node {

    public final byte name;
    public final String target;

    public Node(byte name, String target) {
        this.name = name;
        this.target = target;
    }
}

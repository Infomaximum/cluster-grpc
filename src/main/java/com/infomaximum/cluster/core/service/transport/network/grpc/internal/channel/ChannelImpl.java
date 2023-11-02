package com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel;

import com.infomaximum.cluster.core.service.transport.network.LocationRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.struct.RNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.convert.ConvertProto;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageUpdateNode;
import io.grpc.stub.StreamObserver;

import java.util.List;

public abstract class ChannelImpl implements Channel {

    public final RNode remoteNode;
    protected final StreamObserver<PNetPackage> requestObserver;

    private volatile boolean available;

    protected ChannelImpl(RNode remoteNode, StreamObserver<PNetPackage> requestObserver) {
        this.remoteNode = remoteNode;
        this.requestObserver = requestObserver;
        this.available = true;
    }

    @Override
    public RNode getRemoteNode() {
        return remoteNode;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    public void handleIncomingPacket(PNetPackageUpdateNode value){
        List<LocationRuntimeComponent> components = ConvertProto.convert(remoteNode.node.getRuntimeId(), value);
        remoteNode.setComponents(components);
    }

    public void sent(PNetPackage value){
        requestObserver.onNext(value);
    }

    public void destroy(){
        available = false;
    }
}

package com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel;

import com.infomaximum.cluster.core.service.transport.network.grpc.internal.struct.RNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.convert.ConvertProto;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageHandshake;
import io.grpc.stub.StreamObserver;
public interface Channel {

    RNode getRemoteNode();

    boolean isAvailable();

    public static class Builder{

        private final StreamObserver<PNetPackage> requestObserver;
        private final PNetPackageHandshake remotePackageHandshake;

        public Builder(StreamObserver<PNetPackage> requestObserver, PNetPackageHandshake remotePackageHandshake) {
            this.requestObserver = requestObserver;
            this.remotePackageHandshake = remotePackageHandshake;
        }

        public Channel build(){
            RNode remoteNode = ConvertProto.convert(remotePackageHandshake.getNode());
            return new ChannelImpl(remoteNode, requestObserver);
        }
    }
}

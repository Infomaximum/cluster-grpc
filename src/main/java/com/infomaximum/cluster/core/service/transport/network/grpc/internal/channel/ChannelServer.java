package com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel;

import com.infomaximum.cluster.core.service.transport.network.grpc.internal.struct.RNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.convert.ConvertProto;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageHandshake;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

public class ChannelServer extends ChannelImpl {

    ChannelServer(RNode remoteNode, StreamObserver<PNetPackage> requestObserver) {
        super(remoteNode, requestObserver);
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        ServerCallStreamObserver<PNetPackage> serverCallStreamObserver = (ServerCallStreamObserver<PNetPackage>) requestObserver;
        return serverCallStreamObserver.isReady();
    }

    @Override
    public ChannelType getType() {
        return ChannelType.SERVER;
    }

    public static class Builder {

        private final StreamObserver<PNetPackage> requestObserver;
        private final PNetPackageHandshake remotePackageHandshake;

        public Builder(StreamObserver<PNetPackage> requestObserver, PNetPackageHandshake remotePackageHandshake) {
            this.requestObserver = requestObserver;
            this.remotePackageHandshake = remotePackageHandshake;
        }

        public ChannelServer build(){
            RNode remoteNode = ConvertProto.convert(remotePackageHandshake.getNode());
            return new ChannelServer(remoteNode, requestObserver);
        }
    }
}

package com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel;

import com.infomaximum.cluster.core.service.transport.network.grpc.internal.struct.RNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.convert.ConvertProto;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageHandshakeResponse;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.util.UUID;

public class ChannelClient extends ChannelImpl {

    ChannelClient(UUID uuid, RNode remoteNode, long channelTimeoutMillis, StreamObserver<PNetPackage> requestObserver) {
        super(uuid, remoteNode, channelTimeoutMillis, requestObserver);
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        ClientCallStreamObserver<PNetPackage> clientCallStreamObserver = (ClientCallStreamObserver<PNetPackage>) requestObserver;
        return clientCallStreamObserver.isReady();
    }

    @Override
    public void kill(Throwable throwable) {
        destroy();
        ClientCallStreamObserver<PNetPackage> clientCallStreamObserver = (ClientCallStreamObserver<PNetPackage>) requestObserver;
        clientCallStreamObserver.onError(throwable);
    }

    @Override
    public ChannelType getType() {
        return ChannelType.CLIENT;
    }

    public static class Builder {

        private final UUID uuid;
        private final StreamObserver<PNetPackage> requestObserver;
        private final PNetPackageHandshakeResponse handshakeResponse;
        private final long channelTimeoutMillis;

        public Builder(UUID uuid, StreamObserver<PNetPackage> requestObserver, PNetPackageHandshakeResponse handshakeResponse, long channelTimeoutMillis) {
            this.uuid = uuid;
            this.requestObserver = requestObserver;
            this.handshakeResponse = handshakeResponse;
            this.channelTimeoutMillis = channelTimeoutMillis;
        }

        public ChannelClient build(){
            RNode remoteNode = ConvertProto.convert(handshakeResponse.getNode());
            return new ChannelClient(uuid, remoteNode, channelTimeoutMillis, requestObserver);
        }
    }
}

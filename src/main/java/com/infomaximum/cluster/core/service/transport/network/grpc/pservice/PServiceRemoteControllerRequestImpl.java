package com.infomaximum.cluster.core.service.transport.network.grpc.pservice;

import com.google.protobuf.ByteString;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.grpc.engine.server.GrpcServer;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRemoteControllerRequestArgument;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRemoteControllerRequestResult;
import com.infomaximum.cluster.core.service.transport.network.grpc.utils.serialize.ObjectSerialize;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PServiceRemoteControllerRequestImpl extends PServiceRemoteControllerRequestGrpc.PServiceRemoteControllerRequestImplBase {

    private final static Logger log = LoggerFactory.getLogger(PServiceRemoteControllerRequestImpl.class);

    private final TransportManager transportManager;
    private final GrpcNetworkTransit grpcNetworkTransit;

    public PServiceRemoteControllerRequestImpl(GrpcServer grpcServer) {
        this.transportManager = grpcServer.grpcNetworkTransit.transportManager;
        this.grpcNetworkTransit = grpcServer.grpcNetworkTransit;
    }

    @Override  //StreamObserver<ProfileDescriptorOuterClass.ProfileDescriptor> responseObserver
    public void request(PRemoteControllerRequestArgument request, StreamObserver<PRemoteControllerRequestResult> responseObserver) {

        Object[] args = new Object[request.getArgsCount()];
        for (int i = 0; i < args.length; i++) {
            args[i] = ObjectSerialize.deserialize(request.getArgs(i).toByteArray());
        }

        PRemoteControllerRequestResult.Builder requestResultBuilder = PRemoteControllerRequestResult.newBuilder();
        try {
            Object result = transportManager.localRequest(
                    request.getTargetComponentUniqueId(),
                    request.getRControllerClassName(),
                    request.getMethodName(),
                    args
            );
            requestResultBuilder.setResult(ByteString.copyFrom(ObjectSerialize.serialize(result)));
        } catch (Exception e) {
            requestResultBuilder.setException(ByteString.copyFrom(ObjectSerialize.serialize(e)));
        }

        synchronized (responseObserver) {
            responseObserver.onNext(requestResultBuilder.build());
            responseObserver.onCompleted();
        }
    }
}

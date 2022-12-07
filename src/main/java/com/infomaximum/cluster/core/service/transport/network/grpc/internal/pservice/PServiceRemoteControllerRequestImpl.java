package com.infomaximum.cluster.core.service.transport.network.grpc.internal.pservice;

import com.google.protobuf.ByteString;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine.server.GrpcServer;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceRemoteControllerRequestGrpc;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRemoteControllerRequestArgument;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRemoteControllerRequestResult;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.serialize.ObjectSerialize;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PServiceRemoteControllerRequestImpl extends PServiceRemoteControllerRequestGrpc.PServiceRemoteControllerRequestImplBase {

    private final static Logger log = LoggerFactory.getLogger(PServiceRemoteControllerRequestImpl.class);

    private final TransportManager transportManager;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public PServiceRemoteControllerRequestImpl(GrpcServer grpcServer) {
        this.transportManager = grpcServer.grpcNetworkTransit.transportManager;
        this.uncaughtExceptionHandler = grpcServer.grpcNetworkTransit.getUncaughtExceptionHandler();
    }

    @Override  //StreamObserver<ProfileDescriptorOuterClass.ProfileDescriptor> responseObserver
    public void request(PRemoteControllerRequestArgument request, StreamObserver<PRemoteControllerRequestResult> responseObserver) {
        try {

            Object[] args = new Object[request.getArgsCount()];
            for (int i = 0; i < args.length; i++) {
                args[i] = ObjectSerialize.deserialize(request.getArgs(i).toByteArray(), uncaughtExceptionHandler);
            }

            PRemoteControllerRequestResult.Builder requestResultBuilder = PRemoteControllerRequestResult.newBuilder();
            try {
                Object result = transportManager.localRequest(
                        request.getTargetComponentUniqueId(),
                        request.getRControllerClassName(),
                        request.getMethodName(),
                        args
                );

                //TODO Потенциальный баг, если при сохранении результата произойдет ошибка, мы запишем эту ошибку в ответ, хотя надо упасть
                requestResultBuilder.setResult(ByteString.copyFrom(ObjectSerialize.serialize(result, uncaughtExceptionHandler)));
            } catch (Exception e) {
                requestResultBuilder.setException(ByteString.copyFrom(ObjectSerialize.serialize(e, uncaughtExceptionHandler)));
            }

            synchronized (responseObserver) {
                responseObserver.onNext(requestResultBuilder.build());
                responseObserver.onCompleted();
            }

        } catch (Throwable e) {
            uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
        }
    }
}

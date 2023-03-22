package com.infomaximum.cluster.core.service.transport.network.grpc.internal.pservice;

import com.google.protobuf.ByteString;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.executor.ComponentExecutorTransport;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine.server.GrpcServer;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceRemoteControllerRequestGrpc;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRemoteControllerRequestArgument;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRemoteControllerRequestResult;
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

    @Override
    public void request(PRemoteControllerRequestArgument request, StreamObserver<PRemoteControllerRequestResult> responseObserver) {
        try {
            byte[][] byteArgs = new byte[request.getArgsCount()][];
            for (int i = 0; i < byteArgs.length; i++) {
                byteArgs[i] = request.getArgs(i).toByteArray();
            }
            ComponentExecutorTransport.Result result = transportManager.localRequest(
                    request.getTargetComponentUniqueId(),
                    request.getRControllerClassName(),
                    request.getMethodName(),
                    byteArgs
            );

            PRemoteControllerRequestResult.Builder requestResultBuilder = PRemoteControllerRequestResult.newBuilder();
            if (result.exception() != null) {
                requestResultBuilder.setException(ByteString.copyFrom(result.exception()));
            } else {
                requestResultBuilder.setResult(ByteString.copyFrom(result.value()));
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

package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller;

import com.google.protobuf.ByteString;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine.client.item.GrpcClientItem;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceRemoteControllerRequestGrpc;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRemoteControllerRequestArgument;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRemoteControllerRequestResult;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.serialize.ObjectSerialize;
import com.infomaximum.cluster.exception.ExceptionBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GrpcRemoteControllerRequestItem {

    private final GrpcNetworkTransitImpl grpcNetworkTransit;

    private final byte targetNode;
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    private final GrpcClientItem grpcClientItem;
    private final PServiceRemoteControllerRequestGrpc.PServiceRemoteControllerRequestStub asyncStub;


    public GrpcRemoteControllerRequestItem(GrpcNetworkTransitImpl grpcNetworkTransit, byte targetNode) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.targetNode = targetNode;
        this.uncaughtExceptionHandler = grpcNetworkTransit.getUncaughtExceptionHandler();

        this.grpcClientItem = grpcNetworkTransit.grpcClient.getClient(targetNode);
        this.asyncStub = PServiceRemoteControllerRequestGrpc.newStub(grpcClientItem.channel);
    }

    public Object transitRequest(int targetComponentUniqueId, String rControllerClassName, String methodName, Object[] args) throws Exception {

        PRemoteControllerRequestArgument.Builder builder = PRemoteControllerRequestArgument.newBuilder()
                .setTargetComponentUniqueId(targetComponentUniqueId)
                .setRControllerClassName(rControllerClassName)
                .setMethodName(methodName);
        if (args != null) {
            for (Object arg : args) {
                builder.addArgs(ByteString.copyFrom(ObjectSerialize.serialize(arg, uncaughtExceptionHandler)));
            }
        }
        PRemoteControllerRequestArgument requestArgument = builder.build();


        CompletableFuture completableFuture = new CompletableFuture();

        StreamObserver<PRemoteControllerRequestResult> observerRequest =
                new StreamObserver<PRemoteControllerRequestResult>() {
                    @Override
                    public void onNext(PRemoteControllerRequestResult value) {
                        try {
                            if (!value.getException().isEmpty()) {
                                Throwable throwable = (Throwable) ObjectSerialize.deserialize(value.getException().toByteArray(), uncaughtExceptionHandler);
                                completableFuture.completeExceptionally(throwable);
                            } else {
                                Object result = ObjectSerialize.deserialize(value.getResult().toByteArray(), uncaughtExceptionHandler);
                                completableFuture.complete(result);
                            }
                        } catch (Throwable e) {
                            uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        try {
                            Throwable finalException = throwable;
                            if (throwable instanceof StatusRuntimeException) {
                                StatusRuntimeException statusRuntimeException = (StatusRuntimeException) throwable;

                                ExceptionBuilder exceptionBuilder = grpcNetworkTransit.transportManager.getExceptionBuilder();
                                if (statusRuntimeException.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                                    finalException = exceptionBuilder.buildRemoteComponentUnavailableException(targetNode, targetComponentUniqueId, rControllerClassName, methodName, statusRuntimeException);
                                } else {
                                    finalException = exceptionBuilder.buildTransitRequestException(targetNode, targetComponentUniqueId, rControllerClassName, methodName, statusRuntimeException);
                                }
                            }
                            completableFuture.completeExceptionally(finalException);
                        } catch (Throwable e) {
                            uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
                        }
                    }

                    @Override
                    public void onCompleted() {

                    }
                };
        asyncStub.request(requestArgument, observerRequest);

        try {
            return completableFuture.get();
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }
    }
}

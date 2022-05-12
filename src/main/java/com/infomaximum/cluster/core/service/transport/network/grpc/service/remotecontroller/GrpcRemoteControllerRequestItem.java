package com.infomaximum.cluster.core.service.transport.network.grpc.service.remotecontroller;

import com.google.protobuf.ByteString;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.grpc.engine.client.item.GrpcClientItem;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceRemoteControllerRequestGrpc;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRemoteControllerRequestArgument;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRemoteControllerRequestResult;
import com.infomaximum.cluster.core.service.transport.network.grpc.utils.serialize.ObjectSerialize;
import com.infomaximum.cluster.exception.ExceptionBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class GrpcRemoteControllerRequestItem {

    private final GrpcNetworkTransit grpcNetworkTransit;

    private final byte targetNode;

    private final GrpcClientItem grpcClientItem;
    private final PServiceRemoteControllerRequestGrpc.PServiceRemoteControllerRequestStub asyncStub;


    public GrpcRemoteControllerRequestItem(GrpcNetworkTransit grpcNetworkTransit, byte targetNode) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.targetNode = targetNode;

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
                builder.addArgs(ByteString.copyFrom(ObjectSerialize.serialize(arg)));
            }
        }
        PRemoteControllerRequestArgument requestArgument = builder.build();


        CompletableFuture completableFuture = new CompletableFuture();

        StreamObserver<PRemoteControllerRequestResult> observerRequest =
                new StreamObserver<PRemoteControllerRequestResult>() {
                    @Override
                    public void onNext(PRemoteControllerRequestResult value) {
                        if (!value.getException().isEmpty()) {
                            Throwable throwable = (Throwable) ObjectSerialize.deserialize(value.getException().toByteArray());
                            completableFuture.completeExceptionally(throwable);
                        } else {
                            Object result = ObjectSerialize.deserialize(value.getResult().toByteArray());
                            completableFuture.complete(result);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Throwable finalException = throwable;
                        if (throwable instanceof StatusRuntimeException) {
                            StatusRuntimeException statusRuntimeException = (StatusRuntimeException) throwable;

                            ExceptionBuilder exceptionBuilder = grpcNetworkTransit.transportManager.getExceptionBuilder();
                            if (statusRuntimeException.getStatus().equals(Status.UNAVAILABLE)) {
                                finalException = exceptionBuilder.buildRemoteComponentUnavailableException(targetNode, targetComponentUniqueId, statusRuntimeException);
                            } else {
                                finalException = exceptionBuilder.buildTransitRequestException(targetNode, targetComponentUniqueId, statusRuntimeException);
                            }
                        }
                        completableFuture.completeExceptionally(finalException);
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

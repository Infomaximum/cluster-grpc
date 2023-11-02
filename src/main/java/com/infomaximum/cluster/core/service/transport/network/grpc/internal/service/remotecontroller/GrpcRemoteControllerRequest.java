package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller;

import com.google.protobuf.ByteString;
import com.infomaximum.cluster.core.service.transport.executor.ComponentExecutorTransport;
import com.infomaximum.cluster.core.service.transport.network.RemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channel;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.ChannelImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.*;
import com.infomaximum.cluster.struct.Component;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GrpcRemoteControllerRequest implements RemoteControllerRequest {

    private final static Logger log = LoggerFactory.getLogger(GrpcRemoteControllerRequest.class);

    private final GrpcNetworkTransitImpl grpcNetworkTransit;

    private final AtomicInteger ids;
    private final ConcurrentHashMap<Integer, NetRequest> requests;

    private final ScheduledExecutorService scheduledServiceWaitNetExecute;

    private final ConcurrentHashMap<Integer, StreamObserver<PNetPackage>> waitLocalExecuteRequest;
    private final ScheduledExecutorService scheduledServiceWaitLocalExecute;

    public GrpcRemoteControllerRequest(GrpcNetworkTransitImpl grpcNetworkTransit) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.ids = new AtomicInteger();
        this.requests = new ConcurrentHashMap<>();

        this.scheduledServiceWaitNetExecute = Executors.newSingleThreadScheduledExecutor();
        this.scheduledServiceWaitNetExecute.scheduleWithFixedDelay(() -> checkTimeoutRequest(), 1, grpcNetworkTransit.getTimeoutConfirmationWaitResponse().toMillis()/3, TimeUnit.MILLISECONDS);

        this.waitLocalExecuteRequest = new ConcurrentHashMap<>();
        this.scheduledServiceWaitLocalExecute = Executors.newSingleThreadScheduledExecutor();
        this.scheduledServiceWaitLocalExecute.scheduleWithFixedDelay(() -> sendWaitResponsePackets(), 1, grpcNetworkTransit.getTimeoutConfirmationWaitResponse().toMillis()/3, TimeUnit.MILLISECONDS);
    }

    private int nextPackageId() {
        return ids.updateAndGet(value -> (value == Integer.MAX_VALUE) ? 1 : value + 1);
    }

    @Override
    public ComponentExecutorTransport.Result request(Component sourceComponent, UUID targetNodeRuntimeId, int targetComponentId, String rControllerClassName, int methodKey, byte[][] args) throws Exception {
        int packageId = nextPackageId();
        CompletableFuture<PNetPackageResponse> completableFuture = new CompletableFuture<>();

        //Пробуем найти действующий канал
        ChannelImpl channel;
        while (true) {
            channel = (ChannelImpl) grpcNetworkTransit.getChannels().getChannel(targetNodeRuntimeId);
            if (channel == null) {
                requests.remove(packageId);
                throw grpcNetworkTransit.transportManager.getExceptionBuilder().buildRemoteComponentUnavailableException(targetNodeRuntimeId, targetComponentId, rControllerClassName, methodKey, null);
            }
            requests.put(packageId, new NetRequest(channel, targetComponentId, rControllerClassName, methodKey, new Timeout(countTimeFail()), completableFuture));
            if (channel.isAvailable()) {
                break;
            }
        }

        //Формируем пакет-запрос и его отправляем
        PNetPackageRequest.Builder builderPackageRequest = PNetPackageRequest.newBuilder()
                .setPackageId(packageId)
                .setTargetComponentId(targetComponentId)
                .setRControllerClassName(rControllerClassName)
                .setMethodKey(methodKey);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                builderPackageRequest.addArgs(ByteString.copyFrom(args[i]));
            }
        }
        PNetPackage netPackage = PNetPackage.newBuilder().setRequest(builderPackageRequest).build();
        channel.sent(netPackage);

        PNetPackageResponse netPackageResponse = completableFuture.get();

        if (!netPackageResponse.getException().isEmpty()) {
            return new ComponentExecutorTransport.Result(null, netPackageResponse.getException().toByteArray());
        } else {
            return new ComponentExecutorTransport.Result(netPackageResponse.getResult().toByteArray(), null);
        }
    }

    public void handleIncomingPacket(PNetPackageResponse response) {
        NetRequest netRequest = requests.remove(response.getPackageId());
        if (netRequest == null) {
            log.debug("Incoming unknown response packageId: {}", response.getPackageId());
        } else {
            netRequest.completableFuture().complete(response);
        }
    }

    public void handleIncomingPacket(PNetPackageProcessing response) {
        NetRequest netRequest = requests.get(response.getPackageId());
        if (netRequest == null) {
            log.debug("Incoming unknown processing packageId: {}", response.getPackageId());
        } else {
            netRequest.timeout().timeFail = countTimeFail();
        }
    }

    private long countTimeFail(){
        return System.currentTimeMillis() + grpcNetworkTransit.getTimeoutConfirmationWaitResponse().toMillis();
    }

    public void handleIncomingPacket(PNetPackageRequest request, StreamObserver<PNetPackage> responseObserver) {
        int packageId = request.getPackageId();

        waitLocalExecuteRequest.put(packageId, responseObserver);
        byte[][] byteArgs = new byte[request.getArgsCount()][];
        for (int i = 0; i < byteArgs.length; i++) {
            byteArgs[i] = request.getArgs(i).toByteArray();
        }
        ComponentExecutorTransport.Result result = grpcNetworkTransit.transportManager.localRequest(
                request.getTargetComponentId(),
                request.getRControllerClassName(),
                request.getMethodKey(),
                byteArgs
        );
        waitLocalExecuteRequest.remove(packageId);

        PNetPackageResponse.Builder responseBuilder = PNetPackageResponse.newBuilder()
                .setPackageId(packageId);
        if (result.exception() != null) {
            responseBuilder.setException(ByteString.copyFrom(result.exception()));
        } else {
            responseBuilder.setResult(ByteString.copyFrom(result.value()));
        }
        responseObserver.onNext(PNetPackage.newBuilder().setResponse(responseBuilder).build());
    }

    public void disconnectChannel(Channel channel) {
        for (Map.Entry<Integer, NetRequest> entry : requests.entrySet()) {
            NetRequest netRequest = entry.getValue();
            if (netRequest.channel() == channel) {
                int packageId = entry.getKey();
                fireErrorNetworkRequest(packageId);
            }
        }
    }

    private void checkTimeoutRequest() {
        try {
            long now = System.currentTimeMillis();
            for (Map.Entry<Integer, NetRequest> entry : requests.entrySet()) {
                NetRequest netRequest = entry.getValue();
                if (!netRequest.channel().isAvailable() || now > netRequest.timeout().timeFail) {
                    int packageId = entry.getKey();
                    fireErrorNetworkRequest(packageId);
                }
            }
        } catch (Throwable e) {
            grpcNetworkTransit.getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }
    }

    private void fireErrorNetworkRequest(int packageId) {
        NetRequest netRequest = requests.remove(packageId);
        if (netRequest == null) return;

        Exception exception = grpcNetworkTransit.transportManager.getExceptionBuilder().buildTransitRequestException(
                netRequest.channel().getRemoteNode().node.getRuntimeId(), netRequest.componentId(), netRequest.rControllerClassName(), netRequest.methodKey(), null
        );

        PNetPackageResponse pNetPackageResponse = PNetPackageResponse.newBuilder()
                .setPackageId(packageId)
                .setException(ByteString.copyFrom(
                        grpcNetworkTransit.transportManager.getRemotePackerObject().serialize(null, Throwable.class, exception)
                )).build();

        netRequest.completableFuture().complete(pNetPackageResponse);
    }

    private void sendWaitResponsePackets(){
        for (Map.Entry<Integer, StreamObserver<PNetPackage>> entry : waitLocalExecuteRequest.entrySet()) {
            PNetPackageProcessing pNetPackageProcessing = PNetPackageProcessing.newBuilder()
                    .setPackageId(entry.getKey())
                    .build();
            entry.getValue().onNext(PNetPackage.newBuilder().setResponseProcessing(pNetPackageProcessing).build());
        }
    }

    public void close() {
        scheduledServiceWaitNetExecute.shutdown();
        scheduledServiceWaitLocalExecute.shutdown();
    }
}

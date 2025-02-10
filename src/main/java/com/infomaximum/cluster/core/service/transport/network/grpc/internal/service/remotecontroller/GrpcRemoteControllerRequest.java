package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller;

import com.infomaximum.cluster.core.service.transport.executor.ComponentExecutorTransport;
import com.infomaximum.cluster.core.service.transport.network.RemoteControllerRequest;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channel;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.ChannelImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.PackageBodyUtils;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.PackageLog;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.*;
import com.infomaximum.cluster.struct.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GrpcRemoteControllerRequest implements RemoteControllerRequest {

    private final static Logger log = LoggerFactory.getLogger(GrpcRemoteControllerRequest.class);

    private final static long TIME_CLEAR_REQUEST_EXECUTE = Duration.ofHours(2).toMillis();

    private final GrpcNetworkTransitImpl grpcNetworkTransit;

    private final AtomicInteger ids;
    private final ConcurrentHashMap<Integer, NetRequest> requests;

    private final ConcurrentHashMap<UUID, BodyProcess> bodyProcessMap;

    private final ScheduledExecutorService scheduledServiceWaitNetExecute;

    private final ConcurrentHashMap<WaitLocalExecute, WaitLocalExecuteResult> waitLocalExecuteRequest;
    private final ScheduledExecutorService scheduledServiceWaitLocalExecute;

    public GrpcRemoteControllerRequest(GrpcNetworkTransitImpl grpcNetworkTransit) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.ids = new AtomicInteger();
        this.requests = new ConcurrentHashMap<>();
        this.bodyProcessMap = new ConcurrentHashMap<>();

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
        CompletableFuture<ComponentExecutorTransport.Result> completableFuture = new CompletableFuture<>();

        //Формируем пакет-запрос
        PNetPackageRequest.Builder builderPackageRequest = PNetPackageRequest.newBuilder()
                .setPackageId(packageId)
                .setTargetComponentId(targetComponentId)
                .setRControllerClassName(rControllerClassName)
                .setMethodKey(methodKey);

        //Формируем список пакетов-запросов, с учетом размера аргументов
        PNetPackage[] pNetPackages = PackageBodyUtils.getPackages(args, builderPackageRequest);

        //Отправляем, исходим из того, что, может отправиться несколько копий пакета, на другой стороне нужна фильтрация
        requests.put(packageId, new NetRequest(targetNodeRuntimeId, targetComponentId, rControllerClassName, methodKey, new Timeout(countTimeFail()), completableFuture));
        try {
            for (PNetPackage pNetPackage : pNetPackages) {
                grpcNetworkTransit.getChannels().sendPacketWithRepeat(targetNodeRuntimeId, pNetPackage);
            }
        } catch (Exception e) {
            requests.remove(packageId);
            throw e;
        }

        return completableFuture.get();
    }

    public void handleIncomingPacket(PNetPackageResponse response) {
        int packageId = response.getPackageId();
        NetRequest netRequest = requests.remove(packageId);
        if (netRequest == null) {
            log.debug("Incoming unknown package: {}", PackageLog.toString(response));
        } else {
            PNetPackageBody responseBody = response.getResult();
            UUID uuid = PackageBodyUtils.getUuid(responseBody);
            if (uuid != null) {
                BodyProcess bodyProcess = bodyProcessMap.computeIfAbsent(uuid,
                        u -> new BodyProcess(uuid, responseBody.getTotal()));
                bodyProcess.setPackageId(packageId);
                bodyProcess.setEndConsumer(
                        body -> {
                            NetRequest completeRequest = requests.remove(packageId);
                            // Если Request завершился по тайм-ауту, то в мапе его уже не будет
                            if (completeRequest != null) {
                                completeRequest.completableFuture().complete(new ComponentExecutorTransport.Result(body, null));
                            }
                            bodyProcessMap.remove(uuid);
                        });
                requests.put(packageId, netRequest);
                // bodyProcess.put() должен выполняться последним, т.к. в нем дергается consumer
                bodyProcess.put(responseBody);
            } else {
                ComponentExecutorTransport.Result result = !response.getException().isEmpty() ?
                        new ComponentExecutorTransport.Result(null, response.getException().toByteArray()) :
                        new ComponentExecutorTransport.Result(response.getResult().getBody().toByteArray(), null);
                netRequest.completableFuture().complete(result);
            }
        }
    }

    public void handleIncomingPacket(PNetPackageProcessing response) {
        NetRequest netRequest = requests.get(response.getPackageId());
        if (netRequest == null) {
            log.debug("Incoming unknown package: {}", PackageLog.toString(response));
        } else {
            netRequest.timeout().timeFail = countTimeFail();
        }
    }

    private long countTimeFail(){
        return System.currentTimeMillis() + grpcNetworkTransit.getTimeoutConfirmationWaitResponse().toMillis();
    }

    public void handleIncomingPacket(PNetPackageBody packageBody) {
        UUID uuid = new UUID(packageBody.getUuidLeastSigBits(), packageBody.getUuidMostSigBits());
        BodyProcess bodyProcess = bodyProcessMap.computeIfAbsent(uuid,
                u -> new BodyProcess(uuid, packageBody.getTotal()));
        bodyProcess.put(packageBody);
        if (!bodyProcess.isEnd()) {
            NetRequest netRequest = requests.get(bodyProcess.getPackageId());
            if (netRequest != null) {
                netRequest.timeout().timeFail = countTimeFail();
            }
        }
    }

    public void handleIncomingPacket(PNetPackageRequest request, ChannelImpl channel) {
        UUID remoteNodeRuntimeId = channel.remoteNode.node.getRuntimeId();
        int packageId = request.getPackageId();
        WaitLocalExecute waitLocalExecute = new WaitLocalExecute(remoteNodeRuntimeId, packageId);
        WaitLocalExecuteResult waitLocalExecuteResult = new WaitLocalExecuteResult();

        //Механизм проверки, что это не дублирующий пакет
        synchronized (waitLocalExecuteRequest) {
            if (waitLocalExecuteRequest.contains(waitLocalExecute)) {
                log.debug("Duplicate packet, ignore: {}", PackageLog.toString(request));
                return;
            }
            waitLocalExecuteRequest.put(waitLocalExecute, waitLocalExecuteResult);
        }
        ComponentExecutorTransport.Result result;
        try {
            byte[][] byteArgs = getArgs(request);
            result = grpcNetworkTransit.transportManager.localRequest(
                    request.getTargetComponentId(),
                    request.getRControllerClassName(),
                    request.getMethodKey(),
                    byteArgs
            );
        } catch (TimeoutException e) {
            Exception exception = grpcNetworkTransit.transportManager.getExceptionBuilder().buildTransitRequestException(
                    remoteNodeRuntimeId,
                    request.getTargetComponentId(),
                    request.getRControllerClassName(),
                    request.getMethodKey(),
                    new RuntimeException("Timeout arguments wait.")
            );
            result = new ComponentExecutorTransport.Result(null,
                    grpcNetworkTransit.transportManager.getRemotePackerObject().serialize(null, Throwable.class, exception));
        }

        //Ставим флаг, что запрос выполнился
        waitLocalExecuteResult.setEndTime(Instant.now());

        PNetPackageResponse.Builder responseBuilder = PNetPackageResponse.newBuilder()
                .setPackageId(packageId);

        PNetPackage[] packages = PackageBodyUtils.getPackages(result, responseBuilder);
        for (PNetPackage pNetPackage : packages) {
            try {
                grpcNetworkTransit.getChannels().sendPacketWithRepeat(remoteNodeRuntimeId, pNetPackage);
            } catch (Exception e) {
                log.debug("Exception send package: {}", PackageLog.toString(pNetPackage), e);
                // Если мы не смогли отправить часть, то отправлять остальную бессмысленно
                break;
            }
        }
    }

    public byte[][] getArgs(PNetPackageRequest request) throws TimeoutException {
        int argsWaitCount = 0;
        for (PNetPackageBody pNetPackageBody : request.getArgsList()) {
            if (PackageBodyUtils.getUuid(pNetPackageBody) != null) {
                argsWaitCount++;
            }
        }
        CountDownLatch countDownLatch = (argsWaitCount != 0) ? new CountDownLatch(argsWaitCount) : null;
        byte[][] byteArgs = new byte[request.getArgsCount()][];
        for (int i = 0; i < byteArgs.length; i++) {
            PNetPackageBody packageBody = request.getArgs(i);
            UUID uuid = PackageBodyUtils.getUuid(packageBody);
            if (uuid != null) {
                final int index = i;
                BodyProcess bodyProcess = bodyProcessMap.computeIfAbsent(uuid, u -> new BodyProcess(uuid, packageBody.getTotal()));
                bodyProcess.setPackageId(request.getPackageId());
                bodyProcess.setEndConsumer(
                        body -> {
                            byteArgs[index] = body;
                            bodyProcessMap.remove(uuid);
                            countDownLatch.countDown();
                        });
            } else {
                byteArgs[i] = packageBody.getBody().toByteArray();
            }
        }
        if (countDownLatch != null) {
            try {
                if (!countDownLatch.await(grpcNetworkTransit.getTimeoutConfirmationWaitResponse().toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new TimeoutException();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return byteArgs;
    }

    private void checkTimeoutRequest() {
        try {
            long now = System.currentTimeMillis();
            for (Map.Entry<Integer, NetRequest> entry : requests.entrySet()) {
                NetRequest netRequest = entry.getValue();
                //Если вышел таймаут - кидаем ошибку
                if (now > netRequest.timeout().timeFail) {
                    int packageId = entry.getKey();
                    fireErrorNetworkRequest(packageId, "Request timed out");
                }
            }
        } catch (Throwable e) {
            grpcNetworkTransit.getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }
    }

    private void fireErrorNetworkRequest(int packageId, String cause) {
        NetRequest netRequest = requests.remove(packageId);
        if (netRequest == null) return;

        UUID remoteNodeRuntimeId = netRequest.targetNodeRuntimeId();
        Exception exception = grpcNetworkTransit.transportManager.getExceptionBuilder().buildTransitRequestException(
                remoteNodeRuntimeId, netRequest.componentId(), netRequest.rControllerClassName(), netRequest.methodKey(),
                new RuntimeException("Fire error network request, packageId: " + packageId + ", cause: " + cause)
        );

        byte[] exceptionBytes = grpcNetworkTransit.transportManager
                .getRemotePackerObject()
                .serialize(null, Throwable.class, exception);
        netRequest.completableFuture().complete(new ComponentExecutorTransport.Result(null, exceptionBytes));
    }

    private void sendWaitResponsePackets(){
        long cleaningTime = System.currentTimeMillis() + TIME_CLEAR_REQUEST_EXECUTE;
        for (Map.Entry<WaitLocalExecute, WaitLocalExecuteResult> entry : waitLocalExecuteRequest.entrySet()) {
            WaitLocalExecute waitLocalExecute = entry.getKey();
            WaitLocalExecuteResult waitLocalExecuteResult = entry.getValue();
            if (waitLocalExecuteResult.getEndTime() == null) {
                try {
                    PNetPackageProcessing pNetPackageProcessing = PNetPackageProcessing.newBuilder()
                            .setPackageId(waitLocalExecute.packageId())
                            .build();
                    PNetPackage pNetPackage = PNetPackage.newBuilder().setResponseProcessing(pNetPackageProcessing).build();
                    grpcNetworkTransit.getChannels().sendPacket(waitLocalExecute.nodeRuntimeId(), pNetPackage, 1);
                } catch (Exception ignore) {}
            } else if (waitLocalExecuteResult.getEndTime().toEpochMilli() < cleaningTime) {
                waitLocalExecuteRequest.remove(waitLocalExecute);
            }
        }
    }

    public void close() {
        scheduledServiceWaitNetExecute.shutdown();
        scheduledServiceWaitLocalExecute.shutdown();
    }
}

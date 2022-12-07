package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecomponent;

import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine.client.item.GrpcClientItem;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.pservice.PServiceRemoteManagerComponentGrpc;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.Empty;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRuntimeComponentInfoList;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.MLogger;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.convert.ConvertRuntimeComponentInfo;
import com.infomaximum.cluster.utils.ExecutorUtil;
import com.infomaximum.cluster.utils.RandomUtil;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class RemoteManagerRuntimeComponentItem {

    private final static Logger log = LoggerFactory.getLogger(RemoteManagerRuntimeComponentItem.class);

    private final static int TIMEOUT_REPEAT_CONNECT = 1000;//Пауза между попытками подключения(в милисекундах)

    private final GrpcNetworkTransitImpl grpcNetworkTransit;

    private final byte node;

    private final GrpcClientItem grpcClientItem;
    private final PServiceRemoteManagerComponentGrpc.PServiceRemoteManagerComponentStub asyncStub;

    private final MLogger mLog;

    private final StreamObserver<PRuntimeComponentInfoList> observerListenerRemoteComponents;

    private Map<Integer, RuntimeComponentInfo> components;

    public RemoteManagerRuntimeComponentItem(GrpcNetworkTransitImpl grpcNetworkTransit, byte node) {
        this.grpcNetworkTransit = grpcNetworkTransit;
        this.node = node;

        this.grpcClientItem = grpcNetworkTransit.grpcClient.getClient(node);
        this.asyncStub = PServiceRemoteManagerComponentGrpc.newStub(grpcClientItem.channel);

        this.mLog = new MLogger(log, 60 * 1000 / TIMEOUT_REPEAT_CONNECT);//Раз в 1 минуту

        this.observerListenerRemoteComponents =
                new StreamObserver<PRuntimeComponentInfoList>() {
                    @Override
                    public void onNext(PRuntimeComponentInfoList value) {
                        try {
                            List<RuntimeComponentInfo> updateComponents = ConvertRuntimeComponentInfo.convert(value);
                            update(updateComponents);

                            mLog.reset();
                        } catch (Throwable t) {
                            grpcNetworkTransit.getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        mLog.warn("Error connect to remote node: {}, exception: {}", grpcClientItem.remoteNode.target, t);

                        ExecutorUtil.executors.execute(() -> {
                            try {
                                Thread.sleep(TIMEOUT_REPEAT_CONNECT);
                            } catch (InterruptedException e) {

                            }
                            reconnect();
                        });
                    }

                    @Override
                    public void onCompleted() {
                        log.warn("Completed connection with remote node: {}, repeat...", grpcClientItem.remoteNode.target);
                        ExecutorUtil.executors.execute(() -> {
                            try {
                                Thread.sleep(TIMEOUT_REPEAT_CONNECT * 2);
                            } catch (InterruptedException e) {

                            }
                            reconnect();
                        });
                    }
                };
        reconnect();

        this.components = Collections.emptyMap();
    }

    private static boolean equals(Map<Integer, RuntimeComponentInfo> components1, Map<Integer, RuntimeComponentInfo> components2) {
        if (components1.size() != components2.size()) return false;
        for (Map.Entry<Integer, RuntimeComponentInfo> entry1 : components1.entrySet()) {
            RuntimeComponentInfo component2 = components2.get(entry1.getKey());
            if (component2 == null) {
                return false;
            }
            if (!component2.uuid.equals(entry1.getValue().uuid)) {
                return false;
            }
        }
        return true;
    }

    private static String toString(Map<Integer, RuntimeComponentInfo> components) {
        return components.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue().uuid)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private void reconnect() {
        asyncStub.listenerRemoteComponents(Empty.newBuilder().build(), observerListenerRemoteComponents);
    }

    private void update(List<RuntimeComponentInfo> values) {
        Map<Integer, RuntimeComponentInfo> updateComponents = new HashMap<Integer, RuntimeComponentInfo>();
        for (RuntimeComponentInfo runtimeComponentInfo : values) {
            updateComponents.put(runtimeComponentInfo.uniqueId, runtimeComponentInfo);
        }

        if (!equals(components, updateComponents)) {
            log.info("Update remote components from node: {}, components: {}", grpcNetworkTransit.getNode(), node, toString(updateComponents));
        }
        this.components = updateComponents;
    }

    public Collection<RuntimeComponentInfo> getComponents() {
        return components.values();
    }

    public RuntimeComponentInfo get(int uniqueId) {
        for (Map.Entry<Integer, RuntimeComponentInfo> entry : components.entrySet()) {
            RuntimeComponentInfo runtimeComponentInfo = entry.getValue();
            if (uniqueId == runtimeComponentInfo.uniqueId) {
                return runtimeComponentInfo;
            }
        }
        return null;
    }

    public RuntimeComponentInfo find(String uuid, Class<? extends RController> remoteControllerClazz) {
        List<RuntimeComponentInfo> items = new ArrayList<>();
        for (Map.Entry<Integer, RuntimeComponentInfo> entry : components.entrySet()) {
            RuntimeComponentInfo runtimeComponentInfo = entry.getValue();
            String runtimeComponentUuid = runtimeComponentInfo.uuid;
            if (runtimeComponentUuid.equals(uuid)) {
                items.add(runtimeComponentInfo);
            }
        }
        if (items.isEmpty()) {
            return null;
        } else {
            return items.get(RandomUtil.random.nextInt(items.size()));
        }
    }

    public Collection<RuntimeComponentInfo> find(Class<? extends RController> remoteControllerClazz) {
        List<RuntimeComponentInfo> items = new ArrayList<>();
        for (Map.Entry<Integer, RuntimeComponentInfo> entry : components.entrySet()) {
            RuntimeComponentInfo runtimeComponentInfo = entry.getValue();
            if (runtimeComponentInfo.getClassNameRControllers().contains(remoteControllerClazz.getName())) {
                items.add(runtimeComponentInfo);
            }
        }
        return items;
    }

}
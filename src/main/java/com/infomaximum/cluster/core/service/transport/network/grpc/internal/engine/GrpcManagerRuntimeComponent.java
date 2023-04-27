package com.infomaximum.cluster.core.service.transport.network.grpc.internal.engine;

import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.core.service.transport.network.ManagerRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecomponent.RemoteManagerRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.local.LocalManagerRuntimeComponent;
import com.infomaximum.cluster.utils.GlobalUniqueIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public class GrpcManagerRuntimeComponent implements ManagerRuntimeComponent {

    private final static Logger log = LoggerFactory.getLogger(GrpcManagerRuntimeComponent.class);

    private final byte currentNode;
    public final LocalManagerRuntimeComponent localManagerRuntimeComponent;
    private final RemoteManagerRuntimeComponent remoteManagerRuntimeComponent;

    public GrpcManagerRuntimeComponent(GrpcNetworkTransitImpl grpcNetworkTransit) {
        this.currentNode = grpcNetworkTransit.getNode();
        this.localManagerRuntimeComponent = new LocalManagerRuntimeComponent();
        this.remoteManagerRuntimeComponent = new RemoteManagerRuntimeComponent(grpcNetworkTransit);
    }

    @Override
    public void registerComponent(RuntimeComponentInfo subSystemInfo) {
        localManagerRuntimeComponent.registerComponent(subSystemInfo);
    }

    @Override
    public boolean unRegisterComponent(int uniqueId) {
        return localManagerRuntimeComponent.unRegisterComponent(uniqueId);
    }

    @Override
    public Collection<RuntimeComponentInfo> getComponents() {
        ArrayList<RuntimeComponentInfo> components = new ArrayList<>();
        components.addAll(localManagerRuntimeComponent.getComponents());
        components.addAll(remoteManagerRuntimeComponent.getComponents());
        return components;
    }

    @Override
    public RuntimeComponentInfo get(int uniqueId) {
        byte node = GlobalUniqueIdUtils.getNode(uniqueId);
        if (node == currentNode) {
            return localManagerRuntimeComponent.get(uniqueId);
        } else {
            return remoteManagerRuntimeComponent.get(uniqueId);
        }
    }

    @Override
    public RuntimeComponentInfo find(String uuid) {
        RuntimeComponentInfo runtimeComponentInfo = localManagerRuntimeComponent.find(uuid);
        if (runtimeComponentInfo == null) {
            runtimeComponentInfo = remoteManagerRuntimeComponent.find(uuid);
        }
        return runtimeComponentInfo;
    }

    @Override
    public Collection<RuntimeComponentInfo> find(Class<? extends RController> remoteControllerClazz) {
        ArrayList<RuntimeComponentInfo> components = new ArrayList<>();
        components.addAll(localManagerRuntimeComponent.find(remoteControllerClazz));
        components.addAll(remoteManagerRuntimeComponent.find(remoteControllerClazz));
        return components;
    }
}

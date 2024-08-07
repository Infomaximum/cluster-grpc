package com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.convert;

import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.core.service.transport.network.LocationRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.struct.RNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageHandshakeNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageUpdateNode;
import com.infomaximum.cluster.struct.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class ConvertProto {

    private final static Logger log = LoggerFactory.getLogger(ConvertProto.class);

    public static RNode convert(PNetPackageHandshakeNode handshakeNode) {
        UUID nodeRuntimeId = new UUID(handshakeNode.getRuntimeIdMostSigBits(), handshakeNode.getRuntimeIdLeastSigBits());

        Node node = new GrpcNode.Builder(handshakeNode.getName(), nodeRuntimeId).build();

        List<LocationRuntimeComponent> components = new ArrayList<>();
        for (int i = 0; i < handshakeNode.getPNetPackageComponentsCount(); i++) {
            PNetPackageComponent netComponent = handshakeNode.getPNetPackageComponents(i);
            components.add(convert(nodeRuntimeId, netComponent));
        }
        return new RNode(node, components);
    }

    public static List<LocationRuntimeComponent> convert(UUID nodeRuntimeId, PNetPackageUpdateNode packageUpdateNode) {
        List<LocationRuntimeComponent> components = new ArrayList<>();
        for (int i = 0; i < packageUpdateNode.getPNetPackageComponentsCount(); i++) {
            PNetPackageComponent netComponent = packageUpdateNode.getPNetPackageComponents(i);
            components.add(convert(nodeRuntimeId, netComponent));
        }
        return components;
    }

    private static LocationRuntimeComponent convert(UUID nodeRuntimeId, PNetPackageComponent netComponent) {
        String uuid = netComponent.getUuid();
        int id = netComponent.getId();
        Version version = "".equals(netComponent.getVersion()) ? null : Version.parse(netComponent.getVersion());

        HashSet<Class<? extends RController>> classRControllers = new HashSet<>();
        for (int i = 0; i < netComponent.getClassNameRControllersCount(); i++) {
            String classNameRController = netComponent.getClassNameRControllers(i);
            try {
                classRControllers.add(
                        (Class<? extends RController>) Class.forName(classNameRController)
                );
            } catch (ClassNotFoundException cnfe) {
                log.debug("Unknown controller: {} in component: {}", classNameRController, uuid);
            }
        }
        return new LocationRuntimeComponent(nodeRuntimeId, new RuntimeComponentInfo(id, uuid, version, classRControllers));
    }
}

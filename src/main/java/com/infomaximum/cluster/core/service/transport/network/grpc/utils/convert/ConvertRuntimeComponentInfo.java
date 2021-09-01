package com.infomaximum.cluster.core.service.transport.network.grpc.utils.convert;

import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PInfo;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRuntimeComponentInfo;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRuntimeComponentInfoList;
import com.infomaximum.cluster.struct.Info;

import java.util.Collection;
import java.util.HashSet;

public class ConvertRuntimeComponentInfo {


    public static PRuntimeComponentInfoList convert(Collection<RuntimeComponentInfo> value) {
        PRuntimeComponentInfoList.Builder builder = PRuntimeComponentInfoList.newBuilder();
        for (RuntimeComponentInfo item : value) {
            builder.addPRuntimeComponentInfos(ConvertRuntimeComponentInfo.convert(item));
        }
        return builder.build();
    }


    public static PRuntimeComponentInfo convert(RuntimeComponentInfo value) {
        PRuntimeComponentInfo.Builder builder = PRuntimeComponentInfo.newBuilder()
                .setNode(value.node)
                .setUniqueId(value.uniqueId)
                .setInfo(convert(value.info))
                .setIsSingleton(value.isSingleton);
        for (String classNameRController : value.getClassNameRControllers()) {
            builder.addClassNameRControllers(classNameRController);
        }
        return builder.build();
    }

    public static PInfo convert(Info value) {
        PInfo.Builder builder = PInfo.newBuilder()
                .setComponentClass(value.getComponent().getName());
        for (Class dependency : value.getDependencies()) {
            builder.addDependencies(dependency.getName());
        }
        return builder.build();
    }


    public static RuntimeComponentInfo[] convert(PRuntimeComponentInfoList value) {
        RuntimeComponentInfo[] components = new RuntimeComponentInfo[value.getPRuntimeComponentInfosCount()];
        for (int i = 0; i < components.length; i++) {
            components[i] = convert(value.getPRuntimeComponentInfos(i));
        }
        return components;
    }

    public static RuntimeComponentInfo convert(PRuntimeComponentInfo value) {
        HashSet<Class<? extends RController>> classRControllers = new HashSet<>();
        try {
            for (int i = 0; i < value.getClassNameRControllersCount(); i++) {
                classRControllers.add(
                        (Class<? extends RController>) Class.forName(value.getClassNameRControllers(i))
                );
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return new RuntimeComponentInfo(
                (byte) value.getNode(),
                value.getUniqueId(), convert(value.getInfo()), value.getIsSingleton(),
                classRControllers
        );
    }

    public static Info convert(PInfo value) {
        try {
            Info.Builder builder = new Info.Builder(
                    Class.forName(value.getComponentClass())
            );
            for (int i = 0; i < value.getDependenciesCount(); i++) {
                builder.withDependence(Class.forName(value.getDependencies(i)));
            }
            return builder.build();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}

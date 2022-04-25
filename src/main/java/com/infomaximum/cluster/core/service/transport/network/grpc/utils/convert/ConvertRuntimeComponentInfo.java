package com.infomaximum.cluster.core.service.transport.network.grpc.utils.convert;

import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PInfo;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRuntimeComponentInfo;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRuntimeComponentInfoList;
import com.infomaximum.cluster.struct.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class ConvertRuntimeComponentInfo {

    private final static Logger log = LoggerFactory.getLogger(ConvertRuntimeComponentInfo.class);

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


    public static List<RuntimeComponentInfo> convert(PRuntimeComponentInfoList value) {
        List<RuntimeComponentInfo> components = new ArrayList<>();
        for (int i = 0; i < value.getPRuntimeComponentInfosCount(); i++) {
            PRuntimeComponentInfo iValue = value.getPRuntimeComponentInfos(i);
            try {
                components.add(convert(iValue));
            } catch (ClassNotFoundException cnfe) {
                log.debug("Unknown component or its controllers: " + iValue.getInfo().getComponentClass());
            }
        }
        return components;
    }

    public static RuntimeComponentInfo convert(PRuntimeComponentInfo value) throws ClassNotFoundException {
        HashSet<Class<? extends RController>> classRControllers = new HashSet<>();
        for (int i = 0; i < value.getClassNameRControllersCount(); i++) {
            classRControllers.add(
                    (Class<? extends RController>) Class.forName(value.getClassNameRControllers(i))
            );
        }

        return new RuntimeComponentInfo(
                (byte) value.getNode(),
                value.getUniqueId(), convert(value.getInfo()), value.getIsSingleton(),
                classRControllers
        );
    }

    public static Info convert(PInfo value) throws ClassNotFoundException {
        Info.Builder builder = new Info.Builder(
                Class.forName(value.getComponentClass())
        );
        for (int i = 0; i < value.getDependenciesCount(); i++) {
            builder.withDependence(Class.forName(value.getDependencies(i)));
        }
        return builder.build();
    }
}

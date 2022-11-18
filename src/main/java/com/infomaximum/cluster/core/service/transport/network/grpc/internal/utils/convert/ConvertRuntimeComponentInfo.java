package com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.convert;

import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRuntimeComponentInfo;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRuntimeComponentInfoList;
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
                .setUuid(value.uuid)
                .setIsSingleton(value.isSingleton);
        for (String classNameRController : value.getClassNameRControllers()) {
            builder.addClassNameRControllers(classNameRController);
        }
        return builder.build();
    }

    public static List<RuntimeComponentInfo> convert(PRuntimeComponentInfoList value) {
        List<RuntimeComponentInfo> components = new ArrayList<>();
        for (int i = 0; i < value.getPRuntimeComponentInfosCount(); i++) {
            PRuntimeComponentInfo iValue = value.getPRuntimeComponentInfos(i);
            components.add(convert(iValue));
        }
        return components;
    }

    public static RuntimeComponentInfo convert(PRuntimeComponentInfo value) {
        HashSet<Class<? extends RController>> classRControllers = new HashSet<>();
        for (int i = 0; i < value.getClassNameRControllersCount(); i++) {
            String classNameRController = value.getClassNameRControllers(i);
            try {
                classRControllers.add(
                        (Class<? extends RController>) Class.forName(classNameRController)
                );
            } catch (ClassNotFoundException cnfe) {
                log.debug("Unknown controllers: {} in component: {}", classNameRController, value.getUuid());
            }
        }

        return new RuntimeComponentInfo(
                (byte) value.getNode(),
                value.getUniqueId(), value.getUuid(), value.getIsSingleton(),
                classRControllers
        );
    }
}

package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.component.memory.remote.RControllerMemory;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import com.infomaximum.cluster.test.component.custom1.remote.RControllerCustom1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class AboutControllerTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void test(int modeId) throws Exception {
        try (Clusters clusters = new Clusters.Builder(modeId).build()) {
            //Запросы от сluster1
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);

            RControllerMemory rControllerMemory1 = memoryComponent.getRemotes().get(MemoryComponent.class, RControllerMemory.class);
            Assertions.assertEquals(clusters.getCluster1().node.getRuntimeId(), rControllerMemory1.getNodeRuntimeId());
            Assertions.assertEquals("com.infomaximum.cluster.component.memory", rControllerMemory1.getComponentUuid());

            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);
            Assertions.assertEquals(clusters.getCluster2().node.getRuntimeId(), rControllerCustom1.getNodeRuntimeId());
            Assertions.assertEquals("com.infomaximum.cluster.test.component.custom1", rControllerCustom1.getComponentUuid());

            //-------------

            //Запросы от сluster2
            Custom1Component custom1Component = clusters.getCluster2().getAnyLocalComponent(Custom1Component.class);

            RControllerMemory rControllerMemory2 = custom1Component.getRemotes().get(MemoryComponent.class, RControllerMemory.class);
            Assertions.assertEquals(clusters.getCluster1().node.getRuntimeId(), rControllerMemory2.getNodeRuntimeId());
            Assertions.assertEquals("com.infomaximum.cluster.component.memory", rControllerMemory2.getComponentUuid());

            RControllerCustom1 rControllerCustom12 = custom1Component.getRemotes().get(Custom1Component.class, RControllerCustom1.class);
            Assertions.assertEquals(clusters.getCluster2().node.getRuntimeId(), rControllerCustom12.getNodeRuntimeId());
            Assertions.assertEquals("com.infomaximum.cluster.test.component.custom1", rControllerCustom12.getComponentUuid());

        }
    }
}

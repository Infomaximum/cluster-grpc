package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.component.memory.remote.RControllerMemory;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import com.infomaximum.cluster.test.component.custom1.remote.RControllerCustom1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AboutControllerTest {

    @Test
    public void test() throws Exception {
        try (Clusters clusters = new Clusters.Builder().build()) {
            //Запросы от сluster1
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);

            RControllerMemory rControllerMemory1 = memoryComponent.getRemotes().get(MemoryComponent.class, RControllerMemory.class);
            Assertions.assertEquals(1, rControllerMemory1.getNode());
            Assertions.assertEquals("com.infomaximum.cluster.component.memory", rControllerMemory1.getComponentUuid());

            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);
            Assertions.assertEquals(2, rControllerCustom1.getNode());
            Assertions.assertEquals("com.infomaximum.cluster.test.component.custom1", rControllerCustom1.getComponentUuid());

            //-------------

            //Запросы от сluster2
            Custom1Component custom1Component = clusters.getCluster2().getAnyLocalComponent(Custom1Component.class);

            RControllerMemory rControllerMemory2 = custom1Component.getRemotes().get(MemoryComponent.class, RControllerMemory.class);
            Assertions.assertEquals(1, rControllerMemory2.getNode());
            Assertions.assertEquals("com.infomaximum.cluster.component.memory", rControllerMemory2.getComponentUuid());

            RControllerCustom1 rControllerCustom12 = custom1Component.getRemotes().get(Custom1Component.class, RControllerCustom1.class);
            Assertions.assertEquals(2, rControllerCustom12.getNode());
            Assertions.assertEquals("com.infomaximum.cluster.test.component.custom1", rControllerCustom12.getComponentUuid());

        }
    }
}

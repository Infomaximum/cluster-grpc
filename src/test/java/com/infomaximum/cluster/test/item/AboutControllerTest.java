package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.component.memory.remote.RControllerMemory;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom.CustomComponent;
import com.infomaximum.cluster.test.component.custom.remote.RControllerCustom;
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

            RControllerCustom rControllerCustom1 = memoryComponent.getRemotes().get(CustomComponent.class, RControllerCustom.class);
            Assertions.assertEquals(2, rControllerCustom1.getNode());
            Assertions.assertEquals("com.infomaximum.cluster.test.component.custom", rControllerCustom1.getComponentUuid());

            //-------------

            //Запросы от сluster2
            CustomComponent customComponent = clusters.getCluster2().getAnyLocalComponent(CustomComponent.class);

            RControllerMemory rControllerMemory2 = customComponent.getRemotes().get(MemoryComponent.class, RControllerMemory.class);
            Assertions.assertEquals(1, rControllerMemory2.getNode());
            Assertions.assertEquals("com.infomaximum.cluster.component.memory", rControllerMemory2.getComponentUuid());

            RControllerCustom rControllerCustom2 = customComponent.getRemotes().get(CustomComponent.class, RControllerCustom.class);
            Assertions.assertEquals(2, rControllerCustom2.getNode());
            Assertions.assertEquals("com.infomaximum.cluster.test.component.custom", rControllerCustom2.getComponentUuid());

        }
    }
}

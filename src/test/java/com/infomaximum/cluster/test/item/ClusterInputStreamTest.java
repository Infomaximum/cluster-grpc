package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.component.memory.remote.RControllerMemory;
import com.infomaximum.cluster.core.remote.struct.ClusterInputStream;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import com.infomaximum.cluster.test.component.custom1.remote.RControllerCustom1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClusterInputStreamTest {

    @Test
    public void test() throws Exception {
        try (Clusters clusters = new Clusters.Builder().build()) {
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);
            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);

            //ClusterInputStream clusterInputStream = rControllerCustom1.getInputStream(10);

            //byte[] result = clusterInputStream.readAllBytes();
        }
    }
}

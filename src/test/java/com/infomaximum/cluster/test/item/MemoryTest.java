package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.component.memory.remote.RControllerMemory;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom.CustomComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kris on 26.08.16.
 */
public class MemoryTest {

    private final static Logger log = LoggerFactory.getLogger(MemoryTest.class);

    @Test
    public void test1() throws Exception {
        try (Clusters clusters = new Clusters.Builder().build()) {
            CustomComponent customComponent = clusters.getCluster2().getAnyLocalComponent(CustomComponent.class);
            RControllerMemory rControllerMemory = customComponent.getRemotes().get(MemoryComponent.class, RControllerMemory.class);

            String key = "ping";
            String value = "pong";
            rControllerMemory.set(key, value);

            Assertions.assertEquals(value, rControllerMemory.get(key));
        }
    }

}
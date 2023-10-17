package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.component.memory.remote.RControllerMemory;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kris on 26.08.16.
 */
public class MemoryTest {

    private final static Logger log = LoggerFactory.getLogger(MemoryTest.class);

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void test(int modeId) throws Exception {
        try (Clusters clusters = new Clusters.Builder(modeId).build()) {
            Custom1Component custom1Component = clusters.getCluster2().getAnyLocalComponent(Custom1Component.class);
            RControllerMemory rControllerMemory = custom1Component.getRemotes().get(MemoryComponent.class, RControllerMemory.class);

            String key = "ping";
            String value = "pong";
            rControllerMemory.set(key, value);

            Assertions.assertEquals(value, rControllerMemory.get(key));
        }
    }

}

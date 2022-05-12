package com.infomaximum.cluster.test.item.memory;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.component.memory.remote.RControllerMemory;
import com.infomaximum.cluster.test.BaseClusterTest;
import com.infomaximum.cluster.test.component.custom.CustomComponent;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kris on 26.08.16.
 */
public class MemoryTest extends BaseClusterTest {

    private final static Logger log = LoggerFactory.getLogger(MemoryTest.class);

//    @Test
    public void test1() throws Exception {
        CustomComponent customComponent = getCluster2().getAnyLocalComponent(CustomComponent.class);
        RControllerMemory rControllerMemory = customComponent.getRemotes().get(MemoryComponent.class, RControllerMemory.class);

        String key = "ping";
        String value = "pong";

        rControllerMemory.set(key, value);

        Assert.assertEquals(value, rControllerMemory.get(key));
    }

}

package com.infomaximum.cluster.test.item.simple;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.component.memory.remote.RControllerMemory;
import com.infomaximum.cluster.test.BaseClusterTest;
import com.infomaximum.cluster.test.component.custom.CustomComponent;
import com.infomaximum.cluster.test.component.custom.remote.RControllerCustom;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kris on 26.08.16.
 */
public class SimpleTest extends BaseClusterTest {

    private final static Logger log = LoggerFactory.getLogger(SimpleTest.class);

//    @Test
    public void test1() throws Exception {
        MemoryComponent memoryComponent = getCluster1().getAnyLocalComponent(MemoryComponent.class);
        RControllerCustom rControllerCustom = memoryComponent.getRemotes().get(CustomComponent.class, RControllerCustom.class);

        String result = rControllerCustom.empty();

        Assert.assertEquals(null, result);
    }

}

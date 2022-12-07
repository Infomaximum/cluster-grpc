package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.component.memory.remote.RControllerMemory;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom.CustomComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteComponentUnavailableTest {

    private final static Logger log = LoggerFactory.getLogger(MemoryTest.class);

    @Test
    public void test() throws Exception {
        try (Clusters clusters = new Clusters.Builder().build()) {
            CustomComponent customComponent = clusters.getCluster2().getAnyLocalComponent(CustomComponent.class);
            RControllerMemory rControllerMemory = customComponent.getRemotes().get(MemoryComponent.class, RControllerMemory.class);

            String key = "ping";
            String value = "pong";
            rControllerMemory.set(key, value);

            //Останавливаем cluster1
            clusters.getCluster1().close();

//TODO Дописать тест!!!
//            Assertions.assertThrows(ClusterException.class,
//                    ()->{
//                        rControllerMemory.get(key);
//                    });
        }
    }
}
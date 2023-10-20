package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import com.infomaximum.cluster.test.component.custom1.remote.RControllerCustom1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class FailRequestFromDisconnectTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void test(int modeId) throws Exception {
        Clusters clusters = new Clusters.Builder(modeId).build();

        MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);
        RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);

        String response1 = rControllerCustom1.empty();
        Assertions.assertEquals(null, response1);

        AtomicBoolean isOk = new AtomicBoolean(false);
        long timeout = 25000;

        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.submit(() -> {
            Assertions.assertThrows(ClusterException.class,
                    () -> {
                        rControllerCustom1.slowRequest(timeout);
                    });
            isOk.set(true);
        });

        Thread.sleep(1000);

        //Останавливаем cluster2
        clusters.getCluster2().close();

        long t1 = System.currentTimeMillis();
        while (!isOk.get() && System.currentTimeMillis() - t1 < timeout) {
            Thread.sleep(500);
        }

        Assertions.assertTrue(isOk.get());

        //Останавливаем cluster1
        clusters.getCluster1().close();
    }
}

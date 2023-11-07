package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.component.memory.remote.RControllerMemory;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;

public class ConcurrentTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void test(int modeId) throws Exception {
        try (Clusters clusters = new Clusters.Builder(modeId).build()) {
            Custom1Component custom1Component = clusters.getCluster2().getAnyLocalComponent(Custom1Component.class);
            RControllerMemory rControllerMemory = custom1Component.getRemotes().get(MemoryComponent.class, RControllerMemory.class);

            int thread = 50;
            CountDownLatch wait = new CountDownLatch(thread);
            for(int i =0; i< thread; i++) {
                int index = i;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String key = "key" + index;
                            for (int j = 0; j < 100; j++) {
                                String value = String.valueOf(j);
                                rControllerMemory.set(key, value);
                                Assertions.assertEquals(value, rControllerMemory.get(key));
                            }
                        } catch (Throwable e) {
                            Assertions.fail(e);
                        }
                        wait.countDown();
                    }
                }).start();
            }
            wait.await();
        }
    }
}

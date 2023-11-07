package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import com.infomaximum.cluster.test.component.custom1.remote.RControllerCustom1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * Created by kris on 26.08.16.
 */
public class LongRequestTest {

    private final static Logger log = LoggerFactory.getLogger(LongRequestTest.class);

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void test(int modeId) throws Exception {
        Duration timeoutConfirmationWaitResponse = Duration.ofSeconds(5);
        try (Clusters clusters = new Clusters.Builder(modeId).withTimeoutConfirmationWaitResponse(timeoutConfirmationWaitResponse, timeoutConfirmationWaitResponse).build()) {
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);
            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);

            String result = rControllerCustom1.slowRequest(timeoutConfirmationWaitResponse.toMillis() * 2L);

            Assertions.assertEquals("OK", result);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void concurrentTest(int modeId) throws Exception {
        try (Clusters clusters = new Clusters.Builder(modeId).build()) {
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);
            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);

            int thread = 50;
            CountDownLatch wait = new CountDownLatch(thread);
            for(int i =0; i< thread; i++) {
                new Thread(() -> {
                    try {
                        String result = rControllerCustom1.slowRequest(15000);
                        Assertions.assertEquals("OK", result);
                    } catch (Throwable e) {
                        Assertions.fail(e);
                    }
                    wait.countDown();
                }).start();
            }
            wait.await();
        }
    }

}

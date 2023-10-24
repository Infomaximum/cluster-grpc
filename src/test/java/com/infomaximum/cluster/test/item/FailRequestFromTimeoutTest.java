package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import com.infomaximum.cluster.test.component.custom1.remote.RControllerCustom1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class FailRequestFromTimeoutTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void test(int modeId) throws Exception {
        Duration timeoutConfirmationWaitResponse1 = Duration.ofSeconds(10);
        Duration timeoutConfirmationWaitResponse2 = Duration.ofHours(1);
        try (Clusters clusters = new Clusters.Builder(modeId)
                .withTimeoutConfirmationWaitResponse(timeoutConfirmationWaitResponse1, timeoutConfirmationWaitResponse2)
                .build()) {
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);
            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);

            Assertions.assertThrows(ClusterException.class,
                    () -> {
                        rControllerCustom1.slowRequest(15000);
                    });
        }
    }
}

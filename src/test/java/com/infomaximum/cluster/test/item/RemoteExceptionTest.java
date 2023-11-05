package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import com.infomaximum.cluster.test.component.custom1.remote.RControllerCustom1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class RemoteExceptionTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void test(int modeId) throws Exception {
        try (Clusters clusters = new Clusters.Builder(modeId).build()) {
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);
            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);

            Assertions.assertEquals(null, rControllerCustom1.empty());


            String expectedExceptionMessage = "throwException";
            ClusterException throwException = Assertions.assertThrows(ClusterException.class,
                    () -> {
                        rControllerCustom1.throwException(expectedExceptionMessage);
                    });
            Assertions.assertEquals(expectedExceptionMessage, throwException.getMessage());


            Assertions.assertEquals(null, rControllerCustom1.empty());
        }
    }
}

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

public class HttpsTest {

    private final static Logger log = LoggerFactory.getLogger(MemoryTest.class);

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void testSingleSSL(int modeId) throws Exception {
        try (Clusters clusters = new Clusters.Builder(modeId)
                .withServerSSL("ssl/chain1.crt", "ssl/private1.key", Clusters.Builder.Item.CLUSTER1, Clusters.Builder.Item.CLUSTER2)
                .build()) {
            Custom1Component custom1Component = clusters.getCluster2().getAnyLocalComponent(Custom1Component.class);
            RControllerMemory rControllerMemory = custom1Component.getRemotes().get(MemoryComponent.class, RControllerMemory.class);

            String key = "ping";
            String value = "pong";
            rControllerMemory.set(key, value);

            Assertions.assertEquals(value, rControllerMemory.get(key));
        }
    }

    /**
     * Проверяем ситуацию, когда две ноды работают с разными сертификатами - и они не доверяют друг другу
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void testFail(int modeId) {
        try (Clusters clusters = new Clusters.Builder(modeId)
                .withServerSSL("ssl/chain1.crt", "ssl/private1.key", Clusters.Builder.Item.CLUSTER1)
                .withServerSSL("ssl/chain2.crt", "ssl/private2.key", Clusters.Builder.Item.CLUSTER2)
                .build()) {
            Custom1Component custom1Component = clusters.getCluster2().getAnyLocalComponent(Custom1Component.class);

            Assertions.assertThrows(RuntimeException.class, () -> {
                custom1Component.getRemotes().get(MemoryComponent.class, RControllerMemory.class);
            });
        }
    }

    /**
     * Проверяем ситуацию, когда две ноды работают с разными сертификатами - и у них есть доверие к чужому сертификату
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void testTrustCross(int modeId) throws Exception {
        try (Clusters clusters = new Clusters.Builder(modeId)
                .withServerSSL("ssl/chain1.crt", "ssl/private1.key", "ssl/chain2.crt", Clusters.Builder.Item.CLUSTER1)
                .withServerSSL("ssl/chain2.crt", "ssl/private2.key", "ssl/chain1.crt", Clusters.Builder.Item.CLUSTER2)
                .build()) {
            Custom1Component custom1Component = clusters.getCluster2().getAnyLocalComponent(Custom1Component.class);
            RControllerMemory rControllerMemory = custom1Component.getRemotes().get(MemoryComponent.class, RControllerMemory.class);

            String key = "ping";
            String value = "pong";
            rControllerMemory.set(key, value);

            Assertions.assertEquals(value, rControllerMemory.get(key));
        }
    }
}

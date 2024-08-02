package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import com.infomaximum.cluster.test.component.custom1.remote.RControllerCustom1;
import com.infomaximum.cluster.test.component.custom1.remote.RControllerCustom1Impl;
import com.infomaximum.cluster.utils.RandomUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static com.infomaximum.cluster.test.component.custom1.remote.RControllerCustom1Impl.HEAVY_ARG;

public class HeavyRequestTest {

    @ParameterizedTest
    @ValueSource(ints = {1,2,3,4})
    public void heavyArgTest(int modeId) {
        try (Clusters clusters = new Clusters.Builder(modeId).build()) {
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);
            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);

            String result = rControllerCustom1.heavyArgRequest(HEAVY_ARG);
            Assertions.assertEquals("OK", result);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1,2,3,4})
    public void manyHeavyArgTest1(int modeId) {
        try (Clusters clusters = new Clusters.Builder(modeId).build()) {
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);
            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);

            String result = rControllerCustom1.sumArg(HEAVY_ARG, "test", HEAVY_ARG);
            Assertions.assertTrue(result.startsWith(HEAVY_ARG));
            Assertions.assertTrue(result.contains("test"));
            Assertions.assertTrue(result.endsWith(HEAVY_ARG));
            Assertions.assertEquals(HEAVY_ARG + "test" + HEAVY_ARG, result);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1,2,3,4})
    public void manyHeavyArgTest2(int modeId) {
        try (Clusters clusters = new Clusters.Builder(modeId).build()) {
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);
            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);

            byte[] bytes = new byte[1024 * 1024];
            RandomUtil.random.nextBytes(bytes);
            String arg1 = new String(bytes, StandardCharsets.UTF_8);

            bytes = new byte[1024];
            RandomUtil.random.nextBytes(bytes);
            String arg2 = new String(bytes, StandardCharsets.UTF_8);

            bytes = new byte[2 * 1024 * 1024];
            RandomUtil.random.nextBytes(bytes);
            String arg3 = new String(bytes, StandardCharsets.UTF_8);

            String result = rControllerCustom1.sumArg(arg1, arg2, arg3);
            Assertions.assertTrue(result.contains(arg1));
            Assertions.assertTrue(result.contains(arg2));
            Assertions.assertTrue(result.contains(arg3));
            Assertions.assertEquals(arg1 + arg2 + arg3, result);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1,2,3,4})
    public void heavyResponseTest(int modeId) {
        try (Clusters clusters = new Clusters.Builder(modeId).build()) {
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);
            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);

            String result = rControllerCustom1.heavyArgRequest("test");
            Assertions.assertEquals(RControllerCustom1Impl.HEAVY_STR, result);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1,2,3,4})
    public void heavyArgAndHeavyResponseTest(int modeId) {
        try (Clusters clusters = new Clusters.Builder(modeId).build()) {
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);
            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);

            String result = rControllerCustom1.sumArg(HEAVY_ARG, "test", null);
            Assertions.assertEquals(HEAVY_ARG + "testnull", result);
        }
    }
}

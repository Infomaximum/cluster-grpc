package com.infomaximum.cluster.test.handshake;

import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.test.Clusters;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import com.infomaximum.cluster.test.component.custom1.remote.RControllerCustom1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

/**
 * Regression-тест: при асимметричных {@code waitResponseTimeout} на двух узлах handshake
 * передаёт declared timeout инициатора на сторону сервера, и обе стороны используют
 * согласованное значение для ping-pong и KA-cadence. В результате запрос, длительность
 * которого больше клиентского тайм-аута, успешно завершается — peer вовремя шлёт keep-alive,
 * deadline инициатора продлевается, и ответ доходит.
 */
public class HandshakeAlignedSlowRequestTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void test(int modeId) throws Exception {
        Duration waitResponseTimeout1 = Duration.ofSeconds(10);
        Duration waitResponseTimeout2 = Duration.ofHours(1);
        try (Clusters clusters = new Clusters.Builder(modeId)
                .withWaitResponseTimeout(waitResponseTimeout1, waitResponseTimeout2)
                .build()) {
            MemoryComponent memoryComponent = clusters.getCluster1().getAnyLocalComponent(MemoryComponent.class);
            RControllerCustom1 rControllerCustom1 = memoryComponent.getRemotes().get(Custom1Component.class, RControllerCustom1.class);

            // Запрос длительностью 15с при клиентском timeout 10с — без handshake-sync упал бы по timeout,
            // с handshake-sync peer шлёт KA каждые 10s/3 ≈ 3.3с, deadline продлевается, запрос успешен.
            Assertions.assertDoesNotThrow(() -> rControllerCustom1.slowRequest(15000));
        }
    }
}

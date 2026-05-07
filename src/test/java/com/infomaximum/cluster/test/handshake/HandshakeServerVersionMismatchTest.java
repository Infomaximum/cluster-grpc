package com.infomaximum.cluster.test.handshake;

import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcProtocolVersion;
import com.infomaximum.cluster.test.Clusters;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет server-side валидацию совпадения версии транспортного протокола
 * cluster-grpc при handshake: подключение клиента с несовпадающей версией
 * должно быть отвергнуто сервером, ChannelServer не регистрируется.
 */
public class HandshakeServerVersionMismatchTest {

    /**
     * Клиент с версией ниже серверной (например, нода старого выпуска
     * подключается к обновлённому стенду) отвергается сервером — канал
     * не регистрируется ни на клиенте, ни на сервере.
     */
    @Test
    void serverRejectsClientWithOlderProtocolVersion() throws Exception {
        try (Clusters clusters = new Clusters.Builder(Clusters.CommunicationMode.ONE_WAY_1)
                .withProtocolVersion(GrpcProtocolVersion.CURRENT - 1, GrpcProtocolVersion.CURRENT)
                .build()) {
            assertThat(clusters.getCluster1().getRemoteNodes()).isEmpty();
            assertThat(clusters.getCluster2().getRemoteNodes()).isEmpty();
        }
    }

    /**
     * Клиент с версией выше серверной (например, нода нового выпуска
     * подключается к старому стенду) отвергается сервером — симметрично
     * предыдущему сценарию.
     */
    @Test
    void serverRejectsClientWithNewerProtocolVersion() throws Exception {
        try (Clusters clusters = new Clusters.Builder(Clusters.CommunicationMode.ONE_WAY_1)
                .withProtocolVersion(GrpcProtocolVersion.CURRENT + 1, GrpcProtocolVersion.CURRENT)
                .build()) {
            assertThat(clusters.getCluster1().getRemoteNodes()).isEmpty();
            assertThat(clusters.getCluster2().getRemoteNodes()).isEmpty();
        }
    }

    /**
     * Старая нода без поля {@code protocolVersion} в proto после парсинга
     * новой стороной получает proto3-default 0. Сценарий проверяется
     * явно через 0, а не через {@code CURRENT - 1}, чтобы остаться валидным
     * после будущих бампов {@code GrpcProtocolVersion.CURRENT}.
     */
    @Test
    void serverRejectsClientWithZeroProtocolVersion() throws Exception {
        try (Clusters clusters = new Clusters.Builder(Clusters.CommunicationMode.ONE_WAY_1)
                .withProtocolVersion(0, GrpcProtocolVersion.CURRENT)
                .build()) {
            assertThat(clusters.getCluster1().getRemoteNodes()).isEmpty();
            assertThat(clusters.getCluster2().getRemoteNodes()).isEmpty();
        }
    }
}

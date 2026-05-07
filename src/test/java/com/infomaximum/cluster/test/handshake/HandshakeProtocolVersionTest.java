package com.infomaximum.cluster.test.handshake;

import com.google.protobuf.InvalidProtocolBufferException;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcProtocolVersion;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageHandshakeNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет корректность сериализации поля {@code protocolVersion}
 * в handshake-сообщении и поведение builder'а транзита по отношению к версии:
 * round-trip сохранения значения, поведение при получении пакета от ноды старой
 * версии без поля, дефолт и переопределение версии в {@link GrpcNetworkTransit.Builder}.
 */
public class HandshakeProtocolVersionTest {

    /**
     * Сериализация и десериализация {@link PNetPackageHandshakeNode} с явно заданным
     * {@code protocolVersion} возвращает то же значение.
     */
    @Test
    void protocolVersion_roundTrip_preservesValue() throws InvalidProtocolBufferException {
        PNetPackageHandshakeNode original = PNetPackageHandshakeNode.newBuilder()
                .setProtocolVersion(GrpcProtocolVersion.CURRENT)
                .build();

        byte[] bytes = original.toByteArray();
        PNetPackageHandshakeNode parsed = PNetPackageHandshakeNode.parseFrom(bytes);

        assertThat(parsed.getProtocolVersion()).isEqualTo(GrpcProtocolVersion.CURRENT);
    }

    /**
     * Старый клиент, не знающий о поле {@code protocolVersion}, при сериализации
     * не указывает его. Новая сторона при парсинге получает proto3-default для
     * {@code int32} — ноль, что корректно отличается от {@link GrpcProtocolVersion#CURRENT}
     * и обеспечивает обнаружение несовместимости.
     */
    @Test
    void protocolVersion_missingField_returnsZero() throws InvalidProtocolBufferException {
        PNetPackageHandshakeNode original = PNetPackageHandshakeNode.newBuilder()
                .setName("legacy-node")
                .build();

        byte[] bytes = original.toByteArray();
        PNetPackageHandshakeNode parsed = PNetPackageHandshakeNode.parseFrom(bytes);

        assertThat(parsed.getProtocolVersion()).isEqualTo(0);
        assertThat(parsed.getProtocolVersion()).isNotEqualTo(GrpcProtocolVersion.CURRENT);
    }

    /**
     * Защита от регрессии: {@link GrpcProtocolVersion#CURRENT} обязан быть положительным,
     * иначе он совпадёт с proto3-дефолтом нуля и потеряется отличие от старых нод
     * без поля, нарушив требование «несовместимы со старыми клиентами».
     */
    @Test
    void currentVersion_isPositive() {
        assertThat(GrpcProtocolVersion.CURRENT).isPositive();
    }

    /**
     * По умолчанию {@link GrpcNetworkTransit.Builder} использует {@link GrpcProtocolVersion#CURRENT}
     * как версию протокола, отправляемую в handshake.
     */
    @Test
    void builder_protocolVersion_defaultsToCurrent() {
        GrpcNetworkTransit.Builder builder = new GrpcNetworkTransit.Builder((thread, throwable) -> {});

        assertThat(builder.getProtocolVersion()).isEqualTo(GrpcProtocolVersion.CURRENT);
    }

    /**
     * {@link GrpcNetworkTransit.Builder#withProtocolVersion(int)} переопределяет
     * значение версии — механизм нужен для эмуляции рассинхрона версий
     * в интеграционных тестах.
     */
    @Test
    void builder_withProtocolVersion_overridesDefault() {
        int customVersion = GrpcProtocolVersion.CURRENT - 1;

        GrpcNetworkTransit.Builder builder = new GrpcNetworkTransit.Builder((thread, throwable) -> {})
                .withProtocolVersion(customVersion);

        assertThat(builder.getProtocolVersion()).isEqualTo(customVersion);
    }
}

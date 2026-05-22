package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcRemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.netpackage.NetPackageHandshakeCreator;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageHandshakeNode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class NetPackageHandshakeCreatorTest {

    /**
     * {@code createRequest} помещает per-target тайм-аут адресата в поле
     * {@code waitResponseTimeoutMillis}, чтобы сервер использовал именно его
     * для своего ping-pong и KA-cadence.
     */
    @Test
    public void createRequest_putsPerTargetTimeoutInHandshake() {
        GrpcNetworkTransitImpl transit = mockTransit(Duration.ofSeconds(20));
        GrpcRemoteNode target = new GrpcRemoteNode.Builder("remote:7001")
                .withWaitResponseTimeout(Duration.ofMinutes(5))
                .build();
        UUID channelUuid = UUID.randomUUID();

        PNetPackage netPackage = NetPackageHandshakeCreator.createRequest(transit, channelUuid, target);
        PNetPackageHandshakeNode node = netPackage.getHandshakeRequest().getNode();

        assertThat(node.getWaitResponseTimeoutMillis())
                .isEqualTo((int) Duration.ofMinutes(5).toMillis());
    }

    /**
     * Если у target'а нет per-target тайм-аута — в handshake уходит глобальный default.
     */
    @Test
    public void createRequest_targetWithoutTimeout_fallsBackToGlobal() {
        GrpcNetworkTransitImpl transit = mockTransit(Duration.ofSeconds(20));
        GrpcRemoteNode target = new GrpcRemoteNode.Builder("remote:7001").build();
        UUID channelUuid = UUID.randomUUID();

        PNetPackage netPackage = NetPackageHandshakeCreator.createRequest(transit, channelUuid, target);
        PNetPackageHandshakeNode node = netPackage.getHandshakeRequest().getNode();

        assertThat(node.getWaitResponseTimeoutMillis())
                .isEqualTo((int) Duration.ofSeconds(20).toMillis());
    }

    /**
     * {@code createResponse} помещает глобальный тайм-аут сервера — у server-side нет
     * per-target знания о подключившемся клиенте.
     */
    @Test
    public void createResponse_putsGlobalTimeoutInHandshake() {
        GrpcNetworkTransitImpl transit = mockTransit(Duration.ofSeconds(45));

        PNetPackage netPackage = NetPackageHandshakeCreator.createResponse(transit);
        PNetPackageHandshakeNode node = netPackage.getHandshakeResponse().getNode();

        assertThat(node.getWaitResponseTimeoutMillis())
                .isEqualTo((int) Duration.ofSeconds(45).toMillis());
    }

    private static GrpcNetworkTransitImpl mockTransit(Duration globalTimeout) {
        GrpcNetworkTransitImpl transit = mock(GrpcNetworkTransitImpl.class, RETURNS_DEEP_STUBS);
        when(transit.getWaitResponseTimeout()).thenReturn(globalTimeout);
        when(transit.getProtocolVersion()).thenReturn(1);
        when(transit.getNode().getName()).thenReturn("node-test");
        when(transit.getNode().getRuntimeId()).thenReturn(UUID.randomUUID());
        when(transit.getManagerRuntimeComponent().getLocalManagerRuntimeComponent().getComponents())
                .thenReturn(Collections.emptyList());
        return transit;
    }
}

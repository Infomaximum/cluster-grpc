package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channels;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.client.Client;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.client.Clients;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackagePing;
import com.infomaximum.cluster.test.Clusters;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class InitChannelUnexpectedPacketTest {

    /**
     * Подаёт {@code ping} в {@code responseObserver} клиента при обнулённом {@code clientChannel}
     * и проверяет, что обработчик неперехваченных исключений не был вызван.
     */
    @Test
    @SuppressWarnings("unchecked")
    void staleNonHandshakePacketIsDroppedWithoutCrash() throws Exception {
        AtomicReference<Throwable> crash = new AtomicReference<>();
        Thread.UncaughtExceptionHandler handler = (t, e) -> crash.set(e);

        try (Clusters clusters = new Clusters.Builder(Clusters.CommunicationMode.ONE_WAY_1, handler).build()) {
            Client client = firstClient(clusters);

            // Эмулируем окно reconnect'а: канал ещё/уже не установлен
            setField(client, "clientChannel", null);
            StreamObserver<PNetPackage> responseObserver =
                    (StreamObserver<PNetPackage>) getField(client, "responseObserver");

            // Отсекаем возможный шум фазы старта — проверяем только реакцию на наш пакет
            crash.set(null);

            PNetPackage stalePing = PNetPackage.newBuilder()
                    .setPing(PNetPackagePing.newBuilder().setTime(1L).build())
                    .build();
            responseObserver.onNext(stalePing);

            assertThat(crash.get()).isNull();
        }
    }

    private static Client firstClient(Clusters clusters) throws Exception {
        GrpcNetworkTransitImpl networkTransit =
                (GrpcNetworkTransitImpl) clusters.getCluster1().getTransportManager().networkTransit;
        Channels channels = networkTransit.getChannels();
        Clients clients = (Clients) getField(channels, "clients");
        Client[] clientArray = (Client[]) getField(clients, "clients");
        return clientArray[0];
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}

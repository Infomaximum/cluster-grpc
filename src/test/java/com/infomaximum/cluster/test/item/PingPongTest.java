package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.core.service.transport.network.grpc.exception.ClusterGrpcPingPongTimeoutException;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.ChannelImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channels;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.service.PingPongService;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackagePing;
import com.infomaximum.cluster.event.CauseNodeDisconnect;
import com.infomaximum.cluster.event.UpdateNodeConnect;
import com.infomaximum.cluster.test.Clusters;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PingPongTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    public void
    testFail(int modeId) throws Exception {
        ListenerUpdateConnect listenerUpdateConnect1 = new ListenerUpdateConnect();
        ListenerUpdateConnect listenerUpdateConnect2 = new ListenerUpdateConnect();
        try (Clusters clusters = new Clusters.Builder(modeId)
                .withPingPongTimeout(Duration.ofSeconds(1), Duration.ofSeconds(1))
                .withListenerUpdateConnect(listenerUpdateConnect1, listenerUpdateConnect2)
                .build()) {

            GrpcNetworkTransitImpl networkTransit1 = (GrpcNetworkTransitImpl) clusters.getCluster1().getTransportManager().networkTransit;
            setPingPongServiceSpy(networkTransit1.getChannels());

            GrpcNetworkTransitImpl networkTransit2 = (GrpcNetworkTransitImpl) clusters.getCluster2().getTransportManager().networkTransit;
            setPingPongServiceSpy(networkTransit2.getChannels());

            //Ждем именно ошибку разрыва по пингу
            listenerUpdateConnect1.getPingPongTimeoutExceptionFuture().get(10, TimeUnit.SECONDS);
            listenerUpdateConnect2.getPingPongTimeoutExceptionFuture().get(10, TimeUnit.SECONDS);
        }
    }

    private void setPingPongServiceSpy(Channels channels) throws Exception {
//        Мокируем метод handleIncomingPing - т.е. не отвечаем на пинг
        PingPongService pingPongServiceSpy = Mockito.spy(channels.getPingPongService());
        Mockito.doNothing().when(pingPongServiceSpy).handleIncomingPing(Mockito.any(ChannelImpl.class), Mockito.any(PNetPackagePing.class));

        Field fieldPingPongService = Channels.class.getDeclaredField("pingPongService");
        fieldPingPongService.setAccessible(true);
        fieldPingPongService.set(channels, pingPongServiceSpy);
    }

    public class ListenerUpdateConnect implements UpdateNodeConnect {

        private final CompletableFuture<Boolean> pingPongTimeoutExceptionFuture;

        public ListenerUpdateConnect() {
            pingPongTimeoutExceptionFuture = new CompletableFuture<Boolean>();
        }

        public CompletableFuture getPingPongTimeoutExceptionFuture() {
            return pingPongTimeoutExceptionFuture;
        }

        @Override
        public void onConnect(Node node) {
        }

        @Override
        public void onDisconnect(Node node, CauseNodeDisconnect cause) {
            if (cause.type == CauseNodeDisconnect.Type.TIMEOUT && cause.throwable!=null && cause.throwable.getClass() == ClusterGrpcPingPongTimeoutException.class) {
                if (!pingPongTimeoutExceptionFuture.isDone()){
                    pingPongTimeoutExceptionFuture.complete(true);
                }
            }
        }
    }
}

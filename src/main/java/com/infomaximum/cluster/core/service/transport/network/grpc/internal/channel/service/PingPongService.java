package com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.service;

import com.infomaximum.cluster.core.service.transport.network.grpc.exception.ClusterGrpcPingPongTimeoutException;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channel;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.ChannelImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.ChannelList;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.utils.ChannelIterator;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackagePing;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackagePong;
import com.infomaximum.cluster.event.CauseNodeDisconnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PingPongService {

    private final static Logger log = LoggerFactory.getLogger(PingPongService.class);

    private final static long DEAD_CHECK_TICK_MILLIS = 1000;

    private final ChannelList channelList;
    private final ScheduledExecutorService scheduledServicePingPongExecute;

    private final Duration interval;

    /**
     * Момент получения последнего pong-пакета по каждому каналу (epoch millis). Заполняется при
     * первой проверке/получении pong'а; чистится при первой проверке после destroy канала.
     */
    private final Map<Channel, Long> lastPongReceivedAtByChannel;

    public PingPongService(ChannelList channelList, Duration interval) {
        this.channelList = channelList;
        this.interval = interval;
        this.lastPongReceivedAtByChannel = new ConcurrentHashMap<>();

        this.scheduledServicePingPongExecute = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scheduledServicePingPongExecute.scheduleWithFixedDelay(this::sendPing, 1, interval.toMillis(), TimeUnit.MILLISECONDS);
        scheduledServicePingPongExecute.scheduleWithFixedDelay(this::checkDeadChannels, 1, DEAD_CHECK_TICK_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void sendPing() {
        try {
            PNetPackage pNetPackagePing = PNetPackage.newBuilder().setPing(
                    PNetPackagePing.newBuilder().setTime(System.currentTimeMillis()).build()
            ).build();

            ChannelIterator iterator = channelList.getChannelIterator();
            while (iterator.hasNext()) {
                Channel channel = iterator.next();
                try {
                    channel.send(pNetPackagePing);
                } catch (Exception e) {
                    killChannelWithException(channel, e);
                }
            }
        } catch (Throwable e) {
            channelList.channels.getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }
    }

    private void checkDeadChannels() {
        try {
            long now = System.currentTimeMillis();
            long intervalMillis = interval.toMillis();

            // Освободить state для каналов, которые больше не активны (destroy / reconnect).
            lastPongReceivedAtByChannel.keySet().removeIf(channel -> !channel.isAvailable());

            ChannelIterator iterator = channelList.getChannelIterator();
            while (iterator.hasNext()) {
                Channel channel = iterator.next();
                long pingPongTimeoutMillis = GrpcPingPongTimeouts.calculate(channel);
                // Lazy-init: первый раз увидели канал, стартуем отсчёт от текущего времени
                long lastPong = lastPongReceivedAtByChannel.computeIfAbsent(channel, c -> now);
                if (isDead(now, lastPong, intervalMillis, pingPongTimeoutMillis)) {
                    killChannelWithTimeout(channel);
                }
            }
        } catch (Throwable e) {
            channelList.channels.getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }
    }

    /**
     * Канал считается мёртвым, если с момента последнего pong'а прошло больше, чем
     * {@code intervalMillis + timeoutMillis}: за это время должен был уйти очередной ping
     * и прийти ответ, плюс RTT.
     *
     * @param nowMillis      текущее время (epoch millis)
     * @param lastPongMillis момент последнего полученного pong'а (epoch millis)
     * @param intervalMillis интервал отправки ping-пакетов в миллисекундах
     * @param timeoutMillis  ping-pong тайм-аут канала в миллисекундах
     * @return {@code true}, если канал считается мёртвым и подлежит закрытию
     */
    public static boolean isDead(long nowMillis, long lastPongMillis, long intervalMillis, long timeoutMillis) {
        long silenceMillis = nowMillis - lastPongMillis;
        long allowedSilenceMillis = intervalMillis + timeoutMillis;
        return silenceMillis > allowedSilenceMillis;
    }

    private void killChannelWithTimeout(Channel channel) {
        log.debug("Kill channel with timeout: {}", channel);
        channelList.killChannel(channel, new CauseNodeDisconnect(CauseNodeDisconnect.Type.TIMEOUT, new ClusterGrpcPingPongTimeoutException()));
        lastPongReceivedAtByChannel.remove(channel);
    }

    private void killChannelWithException(Channel channel, Throwable e) {
        log.debug("Kill channel with exception: {}", channel, e);
        channelList.killChannel(channel, new CauseNodeDisconnect(CauseNodeDisconnect.Type.EXCEPTION, e));
        lastPongReceivedAtByChannel.remove(channel);
    }

    public void handleIncomingPong(ChannelImpl channel, PNetPackagePong pong) {
        lastPongReceivedAtByChannel.put(channel, System.currentTimeMillis());
    }

    public void handleIncomingPing(ChannelImpl channel, PNetPackagePing ping) {
        PNetPackage pNetPackagePong = PNetPackage.newBuilder().setPong(
                PNetPackagePong.newBuilder().setTime(ping.getTime()).build()
        ).build();
        try {
            channel.send(pNetPackagePong);
        } catch (Exception e) {
            log.error("Exception send 'pong' package, channel: {}", channel, e);
        }
    }


    public void close() {
        scheduledServicePingPongExecute.shutdown();
    }

    static class GrpcPingPongTimeouts {

        /** Минимально допустимый тайм-аут. */
        public static final Duration MIN = Duration.ofSeconds(3);

        /** Максимально допустимый тайм-аут. */
        public static final Duration MAX = Duration.ofSeconds(30);

        /** Делитель основного тайм-аута ожидания ответа для получения ping-pong тайм-аута. */
        public static final int DIVISOR = 6;

        private GrpcPingPongTimeouts() {}

        public static long calculate(Channel channel) {
            Duration waitResponseTimeout = Duration.ofMillis(channel.getChannelTimeoutMillis());
            Duration scaled = waitResponseTimeout.dividedBy(DIVISOR);
            if (scaled.compareTo(MIN) < 0) {
                return MIN.toMillis();
            }
            if (scaled.compareTo(MAX) > 0) {
                return MAX.toMillis();
            }
            return scaled.toMillis();
        }
    }
}

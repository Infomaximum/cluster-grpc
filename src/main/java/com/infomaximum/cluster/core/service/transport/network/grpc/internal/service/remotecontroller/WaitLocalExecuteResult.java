package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller;

import java.time.Instant;

public class WaitLocalExecuteResult {

    private volatile Instant endTime;

    /**
     * Интервал отправки keep-alive {@code PNetPackageProcessing} в миллисекундах.
     * Вычисляется как {@code effectiveWaitResponseTimeout / 3} при поступлении запроса.
     */
    private final long kaIntervalMillis;

    /**
     * Момент следующей отправки keep-alive (epoch millis). При создании сразу выставляется в
     * {@code System.currentTimeMillis() + kaIntervalMillis}, чтобы первый KA не уходил мгновенно.
     */
    private long nextKaTimeMillis;

    public WaitLocalExecuteResult(long kaIntervalMillis) {
        this.kaIntervalMillis = kaIntervalMillis;
        this.nextKaTimeMillis = System.currentTimeMillis() + kaIntervalMillis;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public long getNextKaTimeMillis() {
        return nextKaTimeMillis;
    }

    public void scheduleNextKa() {
        this.nextKaTimeMillis = System.currentTimeMillis() + kaIntervalMillis;
    }
}

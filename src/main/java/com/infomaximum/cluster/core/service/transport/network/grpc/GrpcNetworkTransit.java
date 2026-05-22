package com.infomaximum.cluster.core.service.transport.network.grpc;

import com.infomaximum.cluster.NetworkTransit;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.CertificateUtils;

import javax.net.ssl.TrustManagerFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface GrpcNetworkTransit {

    class Builder extends NetworkTransit.Builder {

        public record Server(int port){} //String host

        public static final Duration DEFAULT_WAIT_RESPONSE_TIMEOUT = Duration.ofSeconds(20);

        public static final Duration DEFAULT_PING_PONG_INTERVAL = Duration.ofSeconds(5);

        private String nodeName;
        private Server server;
        private final List<GrpcRemoteNode> targets;

        private Duration waitResponseTimeout;

        private Duration pingPongInterval;

        private int protocolVersion;

        private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
        private byte[] fileCertChain;
        private byte[] filePrivateKey;
        private TrustManagerFactory trustStore;

        public Builder(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
            this.targets = new ArrayList<>();
            this.waitResponseTimeout = DEFAULT_WAIT_RESPONSE_TIMEOUT;
            this.pingPongInterval = DEFAULT_PING_PONG_INTERVAL;
            this.protocolVersion = GrpcProtocolVersion.CURRENT;
            this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        }

        public Builder withNodeName(String nodeName) {
            this.nodeName = nodeName;
            return this;
        }

        public Builder withServer(Server server) {
            this.server = server;
            return this;
        }

        public Builder addTarget(GrpcRemoteNode target) {
            this.targets.add(target);
            return this;
        }

        public Builder withTransportSecurity(byte[] fileCertChain, byte[] filePrivateKey, byte[]... trustCertificates) {
            if (fileCertChain == null) {
                throw new IllegalArgumentException();
            }
            if (filePrivateKey == null) {
                throw new IllegalArgumentException();
            }
            this.fileCertChain = fileCertChain;
            this.filePrivateKey = filePrivateKey;
            trustStore = CertificateUtils.buildTrustStore(fileCertChain, trustCertificates);
            return this;
        }

        public Builder withWaitResponseTimeout(Duration value) {
            if (value.isNegative() || value.isZero()) {
                throw new IllegalArgumentException("waitResponseTimeout must be positive, got: " + value);
            }
            this.waitResponseTimeout = value;
            return this;
        }

        public Builder withPingPongInterval(Duration interval) {
            if (interval.isNegative() || interval.isZero()) {
                throw new IllegalArgumentException("pingPongInterval must be positive, got: " + interval);
            }
            this.pingPongInterval = interval;
            return this;
        }

        /**
         * Переопределяет значение версии транспортного протокола, отправляемое в handshake.
         * По умолчанию используется {@link GrpcProtocolVersion#CURRENT}; явное переопределение
         * требуется только для эмуляции рассинхрона версий в тестах.
         *
         * @param protocolVersion значение, которое нода будет отправлять удалённой стороне
         * @return этот builder для chaining
         */
        public Builder withProtocolVersion(int protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public String getNodeName() {
            return nodeName;
        }

        public Server getServer() {
            return server;
        }

        public List<GrpcRemoteNode> getTargets() {
            return Collections.unmodifiableList(targets);
        }

        public Duration getWaitResponseTimeout() {
            return waitResponseTimeout;
        }

        public Duration getPingPongInterval() {
            return pingPongInterval;
        }

        public int getProtocolVersion() {
            return protocolVersion;
        }

        public GrpcNetworkTransitImpl build(TransportManager transportManager) {
            return new GrpcNetworkTransitImpl(this, transportManager, fileCertChain, filePrivateKey, trustStore, uncaughtExceptionHandler);
        }

    }

}

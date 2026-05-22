package com.infomaximum.cluster.core.service.transport.network.grpc;

import java.time.Duration;

public class GrpcRemoteNode {

    public final String target;

    public final Duration waitResponseTimeout;

    private GrpcRemoteNode(Builder builder) {
        this.target = builder.target;
        this.waitResponseTimeout = builder.waitResponseTimeout;
    }

    public Duration resolveEffectiveWaitResponseTimeout(Duration globalDefault) {
        return waitResponseTimeout != null ? waitResponseTimeout : globalDefault;
    }

    public static class Builder {

        private final String target;
        private Duration waitResponseTimeout;

        public Builder(String target) {
            this.target = target;
        }

        public Builder withWaitResponseTimeout(Duration timeout) {
            if (timeout != null && (timeout.isNegative() || timeout.isZero())) {
                throw new IllegalArgumentException("waitResponseTimeout must be positive, got: " + timeout);
            }
            this.waitResponseTimeout = timeout;
            return this;
        }

        public GrpcRemoteNode build() {
            return new GrpcRemoteNode(this);
        }
    }
}

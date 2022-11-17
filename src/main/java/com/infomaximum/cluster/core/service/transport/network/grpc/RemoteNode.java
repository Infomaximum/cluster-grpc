package com.infomaximum.cluster.core.service.transport.network.grpc;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class RemoteNode {

    public final byte name;
    public final String target;

    private RemoteNode(Builder builder) {
        this.name = builder.name;
        this.target = builder.target;
    }

    public static class Builder {

        private byte name;
        private String target;

        public Builder(byte name, String target) {
            this.name = name;
            this.target = target;
        }

        public RemoteNode build() {
            return new RemoteNode(this);
        }
    }
}

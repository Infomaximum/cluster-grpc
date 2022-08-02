package com.infomaximum.cluster.core.service.transport.network.grpc.struct;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

public class Node {

    public final byte name;
    public final String target;

    public final Certificate certificate;

    private Node(Builder builder) {
        this.name = builder.name;
        this.target = builder.target;

        this.certificate = builder.certificate;
    }

    public static class Builder {

        private byte name;
        private String target;

        private Certificate certificate;

        public Builder(byte name, String target) {
            this.name = name;
            this.target = target;
        }

        public Builder withTransportSecurity(byte[] certificate) {
            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                try (InputStream in = new ByteArrayInputStream(certificate)) {
                    this.certificate = certificateFactory.generateCertificate(in);
                }
            } catch (CertificateException | IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public Node build() {
            return new Node(this);
        }
    }
}

package com.infomaximum.cluster.core.service.transport.network.grpc.struct;


public class Node {

    public final byte name;
    public final String target;

    public final byte[] truststore;
    public final char[] truststorePassword;

    private Node(Builder builder) {
        this.name = builder.name;
        this.target = builder.target;

        this.truststore = builder.truststore;
        this.truststorePassword = builder.truststorePassword;
    }

    public static class Builder {

        private byte name;
        private String target;

        private byte[] truststore;
        private char[] truststorePassword;

        public Builder(byte name, String target) {
            this.name = name;
            this.target = target;
        }

        public Builder withTransportSecurity(byte[] truststore, char[] truststorePassword) {
            this.truststore = truststore;
            this.truststorePassword = truststorePassword;
            return this;
        }

        public Node build() {
            return new Node(this);
        }
    }
}

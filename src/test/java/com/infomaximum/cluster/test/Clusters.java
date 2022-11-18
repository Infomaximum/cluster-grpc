package com.infomaximum.cluster.test;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.ComponentBuilder;
import com.infomaximum.cluster.NetworkTransit;
import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.grpc.RemoteNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.UpdateConnect;
import com.infomaximum.cluster.test.component.custom.CustomComponent;
import com.infomaximum.cluster.test.utils.ReaderResources;
import com.infomaximum.cluster.utils.ExecutorUtil;

public class Clusters implements AutoCloseable {

    private Cluster cluster1;
    private Cluster cluster2;

    public Clusters(NetworkTransit.Builder builderNetworkTransit1, NetworkTransit.Builder builderNetworkTransit2, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        ExecutorUtil.executors.execute(() -> {
            cluster1 = new Cluster.Builder()
                    .withNetworkTransport(builderNetworkTransit1)
                    .withComponentIfNotExist(new ComponentBuilder(MemoryComponent.class))
                    .build();
        });

        ExecutorUtil.executors.execute(() -> {
            cluster2 = new Cluster.Builder()
                    .withNetworkTransport(builderNetworkTransit2)
                    .withComponentIfNotExist(new ComponentBuilder(CustomComponent.class))
                    .build();
        });

        //Ожидаем старта
        while (!(cluster1 != null && cluster2 != null )) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Cluster getCluster1() {
        return cluster1;
    }

    public Cluster getCluster2() {
        return cluster2;
    }

    @Override
    public void close() {
        cluster2.close();
        cluster1.close();
    }

    public static class Builder {

        public enum Item {
            CLUSTER1, CLUSTER2;
        }

        private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

        private final GrpcNetworkTransit.Builder builderNetworkTransit1;
        private final GrpcNetworkTransit.Builder builderNetworkTransit2;

        public Builder() {
            this(
                    new Thread.UncaughtExceptionHandler() {
                        @Override
                        public void uncaughtException(Thread t, Throwable e) {
                            e.printStackTrace();
                        }
                    }
            );
        }

        public Builder(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
            this.uncaughtExceptionHandler = uncaughtExceptionHandler;

            builderNetworkTransit1 = new GrpcNetworkTransit.Builder((byte) 1, 7001, uncaughtExceptionHandler)
                    .addTarget(
                            new RemoteNode.Builder((byte) 1, "localhost:7001").build()
                    )
                    .addTarget(
                            new RemoteNode.Builder((byte) 2, "localhost:7002").build()
                    );

            builderNetworkTransit2 = new GrpcNetworkTransit.Builder((byte) 2, 7002, uncaughtExceptionHandler)
                    .addTarget(
                            new RemoteNode.Builder((byte) 1, "localhost:7001").build()
                    )
                    .addTarget(
                            new RemoteNode.Builder((byte) 2, "localhost:7002").build()
                    );
        }

        public Builder withServerSSL(String fileCrt, String fileKey, Item... clusters) {
            return withServerSSL(fileCrt, fileKey, null, clusters);
        }

        public Builder withServerSSL(String fileCrt, String fileKey, String fileTrustCrt, Item... clusters) {
            byte[][] trustCertificates = (fileTrustCrt!=null)?new byte[][]{ReaderResources.read(fileTrustCrt)}:new byte[0][];
            for (Item cluster: clusters) {
                switch (cluster) {
                    case CLUSTER1 -> {
                        builderNetworkTransit1.withTransportSecurity(
                                ReaderResources.read(fileCrt),
                                ReaderResources.read(fileKey),
                                trustCertificates
                        );
                    }
                    case CLUSTER2 -> {
                        builderNetworkTransit2.withTransportSecurity(
                                ReaderResources.read(fileCrt),
                                ReaderResources.read(fileKey),
                                trustCertificates
                        );
                    }
                }
            }
            return this;
        }

        public Builder withListenerUpdateConnect(UpdateConnect updateConnect) {
            builderNetworkTransit1.withListenerUpdateConnect(updateConnect);
            builderNetworkTransit2.withListenerUpdateConnect(updateConnect);
            return this;
        }

        public Clusters build() {
            return new Clusters(builderNetworkTransit1, builderNetworkTransit2, uncaughtExceptionHandler);
        }
    }

}


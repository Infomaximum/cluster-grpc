package com.infomaximum.cluster.test;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.ComponentBuilder;
import com.infomaximum.cluster.NetworkTransit;
import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcRemoteNode;
import com.infomaximum.cluster.event.UpdateNodeConnect;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import com.infomaximum.cluster.test.utils.FinderFreeHostPort;
import com.infomaximum.cluster.test.utils.ReaderResources;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.ExecutorUtil;

import java.time.Duration;

public class Clusters implements AutoCloseable {

    private Cluster cluster1;
    private Cluster cluster2;

    public Clusters(NetworkTransit.Builder builderNetworkTransit1, UpdateNodeConnect updateNodeConnect1, NetworkTransit.Builder builderNetworkTransit2, UpdateNodeConnect updateNodeConnect2, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        ExecutorUtil.executors.execute(() -> {
            Cluster.Builder clusterBuilder1 = new Cluster.Builder(uncaughtExceptionHandler)
                    .withNetworkTransport(builderNetworkTransit1)
                    .withComponentIfNotExist(new ComponentBuilder(MemoryComponent.class));
            if (updateNodeConnect1 != null) {
                clusterBuilder1.withListenerUpdateConnect(updateNodeConnect1);
            }
            cluster1 = clusterBuilder1.build();
        });

        ExecutorUtil.executors.execute(() -> {
            Cluster.Builder clusterBuilder2 = new Cluster.Builder(uncaughtExceptionHandler)
                    .withNetworkTransport(builderNetworkTransit2)
                    .withComponentIfNotExist(new ComponentBuilder(Custom1Component.class));
            if (updateNodeConnect2 != null) {
                clusterBuilder2.withListenerUpdateConnect(updateNodeConnect2);
            }
            cluster2 = clusterBuilder2.build();
        });

        //Ожидаем старта
        while (!(cluster1 != null && cluster2 != null)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }

        try {
            Thread.sleep(3000);
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

    public enum CommunicationMode {

        ONE_WAY_1(1),
        ONE_WAY_2(2),
        TWO_WAY(3),

        LOOP_WAY(4);

        public final int id;

        CommunicationMode(int id) {
            this.id = id;
        }

        public static CommunicationMode get(int id) {
            for (CommunicationMode item : CommunicationMode.values()) {
                if (item.id == id) return item;
            }
            throw new RuntimeException("Unknown id: " + id);
        }
    }

    public static class Builder {

        private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
        private final GrpcNetworkTransit.Builder builderNetworkTransit1;
        private final GrpcNetworkTransit.Builder builderNetworkTransit2;
        private UpdateNodeConnect updateNodeConnect1;
        private UpdateNodeConnect updateNodeConnect2;

        private Duration timeoutConfirmationWaitResponse1;

        private Duration timeoutConfirmationWaitResponse2;

        private Duration pingPongInterval;
        private Duration pingPongTimeout;

        public Builder(int communicationModeId) {
            this(CommunicationMode.get(communicationModeId));
        }

        public Builder(CommunicationMode mode) {
            this(
                    mode,
                    new Thread.UncaughtExceptionHandler() {
                        @Override
                        public void uncaughtException(Thread t, Throwable e) {
                            e.printStackTrace();
                        }
                    }
            );
        }

        public Builder(CommunicationMode mode, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
            this.uncaughtExceptionHandler = uncaughtExceptionHandler;

            int port1 = FinderFreeHostPort.find();
            int port2 = FinderFreeHostPort.find();

            builderNetworkTransit1 = new GrpcNetworkTransit.Builder(uncaughtExceptionHandler).withNodeName("node1");
            builderNetworkTransit2 = new GrpcNetworkTransit.Builder(uncaughtExceptionHandler).withNodeName("node2");

            if (mode == CommunicationMode.TWO_WAY) {
                builderNetworkTransit1.withServer(new GrpcNetworkTransit.Builder.Server(port1));
                builderNetworkTransit1.addTarget(
                        new GrpcRemoteNode.Builder("localhost:" + port2).build()
                );

                builderNetworkTransit2.withServer(new GrpcNetworkTransit.Builder.Server(port2));
                builderNetworkTransit2.addTarget(
                        new GrpcRemoteNode.Builder("localhost:" + port1).build()
                );
            } else if (mode == CommunicationMode.ONE_WAY_1) {
                builderNetworkTransit1.addTarget(
                        new GrpcRemoteNode.Builder("localhost:" + port2).build()
                );

                builderNetworkTransit2.withServer(new GrpcNetworkTransit.Builder.Server(port2));
            } else if (mode == CommunicationMode.ONE_WAY_2) {
                builderNetworkTransit1.withServer(new GrpcNetworkTransit.Builder.Server(port1));

                builderNetworkTransit2.addTarget(
                        new GrpcRemoteNode.Builder("localhost:" + port1).build()
                );
            } else if (mode == CommunicationMode.LOOP_WAY) {
                builderNetworkTransit1.withServer(new GrpcNetworkTransit.Builder.Server(port1));
                builderNetworkTransit1.addTarget(
                        new GrpcRemoteNode.Builder("localhost:" + port2).build()
                );
                builderNetworkTransit1.addTarget(
                        new GrpcRemoteNode.Builder("localhost:" + port1).build()
                );

                builderNetworkTransit2.withServer(new GrpcNetworkTransit.Builder.Server(port2));
                builderNetworkTransit2.addTarget(
                        new GrpcRemoteNode.Builder("localhost:" + port1).build()
                );
                builderNetworkTransit2.addTarget(
                        new GrpcRemoteNode.Builder("localhost:" + port2).build()
                );
            }
        }

        public Builder withServerSSL(String fileCrt, String fileKey, Item... clusters) {
            return withServerSSL(fileCrt, fileKey, null, clusters);
        }

        public Builder withServerSSL(String fileCrt, String fileKey, String fileTrustCrt, Item... clusters) {
            byte[][] trustCertificates = (fileTrustCrt != null) ? new byte[][]{ReaderResources.read(fileTrustCrt)} : new byte[0][];
            for (Item cluster : clusters) {
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

        public Builder withListenerUpdateConnect(UpdateNodeConnect updateNodeConnect1, UpdateNodeConnect updateNodeConnect2) {
            this.updateNodeConnect1 = updateNodeConnect1;
            this.updateNodeConnect2 = updateNodeConnect2;
            return this;
        }

        public Builder withTimeoutConfirmationWaitResponse(Duration timeoutConfirmationWaitResponse1, Duration timeoutConfirmationWaitResponse2) {
            this.timeoutConfirmationWaitResponse1 = timeoutConfirmationWaitResponse1;
            this.timeoutConfirmationWaitResponse2 = timeoutConfirmationWaitResponse2;
            return this;
        }

        public Builder withPingPongTimeout(Duration interval, Duration timeout) {
            this.pingPongInterval = interval;
            this.pingPongTimeout = timeout;
            return this;
        }

        public Clusters build() {
            if (timeoutConfirmationWaitResponse1 != null) {
                builderNetworkTransit1.withTimeoutConfirmationWaitResponse(timeoutConfirmationWaitResponse1);
            }
            if (timeoutConfirmationWaitResponse2 != null) {
                builderNetworkTransit2.withTimeoutConfirmationWaitResponse(timeoutConfirmationWaitResponse2);
            }
            if (pingPongInterval != null || pingPongTimeout != null) {
                builderNetworkTransit1.withPingPongTimeout(pingPongInterval, pingPongTimeout);
                builderNetworkTransit2.withPingPongTimeout(pingPongInterval, pingPongTimeout);
            }
            return new Clusters(builderNetworkTransit1, updateNodeConnect1, builderNetworkTransit2, updateNodeConnect2, uncaughtExceptionHandler);
        }

        public enum Item {
            CLUSTER1, CLUSTER2;
        }
    }
}


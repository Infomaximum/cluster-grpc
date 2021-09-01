package com.infomaximum.cluster.test;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.ComponentBuilder;
import com.infomaximum.cluster.component.memory.MemoryComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNetworkTransit;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.Node;
import com.infomaximum.cluster.core.service.transport.struct.NetworkTransitState;
import com.infomaximum.cluster.test.component.custom.CustomComponent;
import com.infomaximum.cluster.utils.ExecutorUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;


/**
 * Created by kris on 22.04.17.
 * integrationtest_subsystems@leeching.core
 */
public abstract class BaseClusterTest {

    private static GrpcNetworkTransit.Builder builderGrpcNetworkTransit1;
    private static Cluster cluster1;

    private static GrpcNetworkTransit.Builder builderGrpcNetworkTransit2;
    private static Cluster cluster2;

    @BeforeClass
    public static void init() {

        ExecutorUtil.executors.execute(() -> {

            builderGrpcNetworkTransit1 = new GrpcNetworkTransit.Builder((byte)1, 7001)
                .addTarget(new Node((byte)2, "localhost:7002"));


            cluster1 = new Cluster.Builder()
                    .withNetworkTransport(builderGrpcNetworkTransit1)
                    .withComponentIfNotExist(new ComponentBuilder(MemoryComponent.class))
                    .build();
        });

        ExecutorUtil.executors.execute(() -> {

            builderGrpcNetworkTransit2 = new GrpcNetworkTransit.Builder((byte)2, 7002)
                .addTarget(new Node((byte)1, "localhost:7001"));

            cluster2 = new Cluster.Builder()
                    .withNetworkTransport(builderGrpcNetworkTransit2)
                    .withComponentIfNotExist(new ComponentBuilder(CustomComponent.class))
                    .build();
        });

        //Ожидаем старта
        while (
                !(cluster1 !=null && cluster1.getTransportManager().networkTransit.getState() == NetworkTransitState.STARTED
                        && cluster2 !=null && cluster2.getTransportManager().networkTransit.getState() == NetworkTransitState.STARTED)
        ) {
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

    public static Cluster getCluster1() {
        return cluster1;
    }

    public static Cluster getCluster2() {
        return cluster2;
    }

    @AfterClass
    public static void destroy() throws IOException {
        cluster2.close();
        cluster1.close();
    }
}

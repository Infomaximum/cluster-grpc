package com.infomaximum.cluster.core.service.transport.network.grpc.engine.client;

import com.infomaximum.cluster.core.service.transport.network.grpc.engine.client.item.GrpcClientItem;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.Node;
import com.infomaximum.cluster.utils.ExecutorUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class GrpcClient implements AutoCloseable {

    private final Map<Byte, GrpcClientItem> clients;

    public GrpcClient(List<Node> targets) {
        this.clients = new HashMap<>();
        for (Node target: targets) {
            this.clients.put(target.name, new GrpcClientItem(target));
        }
    }

    public GrpcClientItem getClient(byte node){
        return clients.get(node);
    }

    @Override
    public void close() {
        CountDownLatch START = new CountDownLatch(clients.size());
        for (Map.Entry<Byte, GrpcClientItem> entry: clients.entrySet()) {
            GrpcClientItem client = entry.getValue();
            ExecutorUtil.executors.execute(() -> {
                client.close();
                START.countDown();
            });
        }
        try {
            START.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

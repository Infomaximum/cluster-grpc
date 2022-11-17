package com.infomaximum.cluster.core.service.transport.network.grpc.engine.client;

import com.infomaximum.cluster.core.service.transport.network.grpc.engine.client.item.GrpcClientItem;
import com.infomaximum.cluster.core.service.transport.network.grpc.RemoteNode;
import com.infomaximum.cluster.utils.ExecutorUtil;

import javax.net.ssl.TrustManagerFactory;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class GrpcClient implements AutoCloseable {

    private final Map<Byte, GrpcClientItem> clients;

    public GrpcClient(List<RemoteNode> targets, byte[] fileCertChain, byte[] filePrivateKey, TrustManagerFactory trustStore) {
        this.clients = new HashMap<>();
        for (RemoteNode target: targets) {
            this.clients.put(target.name, new GrpcClientItem(target, fileCertChain, filePrivateKey, trustStore));
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

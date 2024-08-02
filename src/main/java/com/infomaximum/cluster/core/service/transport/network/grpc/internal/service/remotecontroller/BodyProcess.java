package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.remotecontroller;

import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.PackageBodyUtils;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackageBody;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class BodyProcess {

    public static final int BODY_DELIMITER = 3 * 1024 * 1024;

    private final UUID uuid;
    private int packageId;
    private final PNetPackageBody[] bodyArray;
    private Consumer<byte[]> endConsumer;

    private final AtomicBoolean isAcceptEndConsumer = new AtomicBoolean(false);

    public BodyProcess(UUID uuid, int total) {
        this.uuid = uuid;
        this.bodyArray = new PNetPackageBody[total];
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
    }

    public void setEndConsumer(Consumer<byte[]> endConsumer) {
        this.endConsumer = endConsumer;
    }

    public void put(PNetPackageBody packageBody) {
        bodyArray[packageBody.getPart() - 1] = packageBody;
        if (isEnd()) {
            if (!isAcceptEndConsumer.getAndSet(true)) {
                if (endConsumer == null) {
                    throw new RuntimeException("End consumer is null");
                }
                endConsumer.accept(PackageBodyUtils.getBody(bodyArray));
            }
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getPackageId() {
        return packageId;
    }

    public boolean isEnd() {
        for (PNetPackageBody pNetPackageBody : bodyArray) {
            if (pNetPackageBody == null) {
                return false;
            }
        }
        return true;
    }
}
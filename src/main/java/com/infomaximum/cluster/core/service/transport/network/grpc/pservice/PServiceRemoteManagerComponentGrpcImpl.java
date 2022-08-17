package com.infomaximum.cluster.core.service.transport.network.grpc.pservice;

import com.google.protobuf.Empty;
import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcManagerRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.grpc.engine.server.GrpcServer;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PRuntimeComponentInfoList;
import com.infomaximum.cluster.core.service.transport.network.grpc.utils.convert.ConvertRuntimeComponentInfo;
import com.infomaximum.cluster.core.service.transport.network.local.LocalManagerRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.local.event.EventUpdateLocalComponent;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PServiceRemoteManagerComponentGrpcImpl extends PServiceRemoteManagerComponentGrpc.PServiceRemoteManagerComponentImplBase implements EventUpdateLocalComponent {

    private final static Logger log = LoggerFactory.getLogger(PServiceRemoteManagerComponentGrpcImpl.class);

    private final LocalManagerRuntimeComponent localManagerRuntimeComponent;

    private final List<StreamObserver<PRuntimeComponentInfoList>> listeners;

    public PServiceRemoteManagerComponentGrpcImpl(GrpcServer grpcServer) {
        GrpcManagerRuntimeComponent grpcManagerRuntimeComponent = (GrpcManagerRuntimeComponent) grpcServer.grpcNetworkTransit.getManagerRuntimeComponent();
        this.localManagerRuntimeComponent = grpcManagerRuntimeComponent.localManagerRuntimeComponent;
        this.listeners = new CopyOnWriteArrayList<>();
        localManagerRuntimeComponent.addListener(this);
    }

    @Override
    public void listenerRemoteComponents(Empty request, StreamObserver<PRuntimeComponentInfoList> responseObserver) {
        listeners.add(responseObserver);

        PRuntimeComponentInfoList value = ConvertRuntimeComponentInfo.convert(localManagerRuntimeComponent.getComponents());
        synchronized (responseObserver) {
            responseObserver.onNext(value);
        }
    }

    @Override
    public void registerComponent(RuntimeComponentInfo subSystemInfo) {
        sendUpdateComponents();
    }

    @Override
    public void unRegisterComponent(RuntimeComponentInfo subSystemInfo) {
        sendUpdateComponents();
    }

    private void sendUpdateComponents(){
        PRuntimeComponentInfoList value = ConvertRuntimeComponentInfo.convert(localManagerRuntimeComponent.getComponents());
        for (StreamObserver<PRuntimeComponentInfoList> listener: listeners) {
            synchronized (listener) {
                listener.onNext(value);
            }
        }
    }

    public void close() {
        for (StreamObserver<PRuntimeComponentInfoList> listener: listeners) {
            synchronized (listener) {
                listener.onCompleted();
            }
        }
    }
}

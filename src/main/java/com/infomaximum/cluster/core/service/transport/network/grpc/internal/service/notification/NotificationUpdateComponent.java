package com.infomaximum.cluster.core.service.transport.network.grpc.internal.service.notification;

import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.service.transport.network.grpc.GrpcNode;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channels;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.netpackage.NetPackageHandshakeCreator;
import com.infomaximum.cluster.core.service.transport.network.grpc.struct.PNetPackage;
import com.infomaximum.cluster.core.service.transport.network.local.LocalManagerRuntimeComponent;
import com.infomaximum.cluster.core.service.transport.network.local.event.EventUpdateLocalComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationUpdateComponent implements EventUpdateLocalComponent {

    private final static Logger log = LoggerFactory.getLogger(NotificationUpdateComponent.class);

    private final Channels channels;

    public NotificationUpdateComponent(LocalManagerRuntimeComponent localManagerRuntimeComponent, Channels channels) {
        this.channels = channels;
        localManagerRuntimeComponent.addListener(this);
    }

    @Override
    public void registerComponent(RuntimeComponentInfo subSystemInfo) {
        sentNotification(subSystemInfo);
    }

    @Override
    public void unRegisterComponent(RuntimeComponentInfo subSystemInfo) {
        //TODO: реализовать когда будет PNetPackageStopComponent
    }

    private void sentNotification(RuntimeComponentInfo subSystemInfo) {
        PNetPackage netPackage = NetPackageHandshakeCreator.buildPackageStartComponent(subSystemInfo);
        channels.sendBroadcast(netPackage);
    }
}

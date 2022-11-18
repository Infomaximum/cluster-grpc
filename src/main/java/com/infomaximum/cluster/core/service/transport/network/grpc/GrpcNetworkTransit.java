package com.infomaximum.cluster.core.service.transport.network.grpc;

import com.infomaximum.cluster.NetworkTransit;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.GrpcNetworkTransitImpl;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils.CertificateUtils;

import javax.net.ssl.TrustManagerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public interface GrpcNetworkTransit {

    class Builder extends NetworkTransit.Builder {

        public final byte nodeName;
        public final int port;
        private final List<RemoteNode> targets;
        private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
        private byte[] fileCertChain;
        private byte[] filePrivateKey;
        private TrustManagerFactory trustStore;
        private final List<UpdateConnect> updateConnectListeners;

        public Builder(byte nodeName, int port, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
            this.nodeName = nodeName;
            this.port = port;
            this.targets = new ArrayList<>();
            this.uncaughtExceptionHandler = uncaughtExceptionHandler;
            this.updateConnectListeners = new CopyOnWriteArrayList<>();
        }

        public Builder addTarget(RemoteNode target) {
            if (target.name != nodeName) {
                this.targets.add(target);
            }
            return this;
        }

        public Builder withTransportSecurity(byte[] fileCertChain, byte[] filePrivateKey, byte[]... trustCertificates) {
            if (fileCertChain == null) {
                throw new IllegalArgumentException();
            }
            if (filePrivateKey == null) {
                throw new IllegalArgumentException();
            }
            this.fileCertChain = fileCertChain;
            this.filePrivateKey = filePrivateKey;
            trustStore = CertificateUtils.buildTrustStore(fileCertChain, trustCertificates);
            return this;
        }

        public Builder withListenerUpdateConnect(UpdateConnect updateConnect) {
            updateConnectListeners.add(updateConnect);
            return this;
        }

        public List<RemoteNode> getTargets() {
            return Collections.unmodifiableList(targets);
        }

        public List<UpdateConnect> getUpdateConnectListeners() {
            return Collections.unmodifiableList(updateConnectListeners);
        }

        public GrpcNetworkTransitImpl build(TransportManager transportManager) {
            return new GrpcNetworkTransitImpl(this, transportManager, fileCertChain, filePrivateKey, trustStore, uncaughtExceptionHandler);
        }

    }

}

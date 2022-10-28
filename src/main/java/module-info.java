module com.infomaximum.cluster.grpc {
    requires org.slf4j;
    requires com.infomaximum.cluster;
    requires java.annotation;
    requires com.google.protobuf;
    requires com.google.common;

    exports com.infomaximum.cluster.core.service.transport.network.grpc;
    exports com.infomaximum.cluster.core.service.transport.network.grpc.struct;
}
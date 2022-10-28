module com.infomaximum.cluster.grpc {
    requires org.slf4j;
    requires com.infomaximum.cluster;
    requires javax.annotation.api;
    requires com.google.protobuf;
//    requires com.google.protobuf.protobufjava;
    requires com.google.common;

    exports com.infomaximum.cluster.core.service.transport.network.grpc;
    exports com.infomaximum.cluster.core.service.transport.network.grpc.struct;
}
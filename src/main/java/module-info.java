module com.infomaximum.cluster.grpc {
    requires org.slf4j;
    requires com.infomaximum.cluster;
    requires com.google.guava.guava;
    requires javax.annotation.api;
    requires io.grpc.grpc;
    requires com.google.protobuf.protobufjava;

    exports com.infomaximum.cluster.core.service.transport.network.grpc;
    exports com.infomaximum.cluster.core.service.transport.network.grpc.struct;
}
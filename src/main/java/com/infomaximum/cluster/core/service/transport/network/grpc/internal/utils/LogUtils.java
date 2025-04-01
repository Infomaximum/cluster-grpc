package com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils;

import java.util.StringJoiner;

public class LogUtils {

    public static String toStringStackTrace(Thread thread) {
        StringJoiner out = new StringJoiner(" ", "[", "]");
        for (StackTraceElement item: thread.getStackTrace()) {
            out.add(item.toString());
        }
        return out.toString();
    }
}

package com.infomaximum.cluster.core.service.transport.network.grpc.internal.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorUtil {

    public static final ExecutorService executors = Executors.newCachedThreadPool();

}

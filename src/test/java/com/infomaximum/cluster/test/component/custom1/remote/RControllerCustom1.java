package com.infomaximum.cluster.test.component.custom1.remote;

import com.infomaximum.cluster.core.remote.struct.ClusterInputStream;
import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.exception.ClusterException;

public interface RControllerCustom1 extends RController {
    String empty() throws ClusterException;
    ClusterInputStream getInputStream(int size) throws ClusterException;
    String slowRequest(long time) throws ClusterException;
    String heavyArgRequest(String arg) throws ClusterException;
    String sumArg(String arg1, String arg2, String arg3) throws ClusterException;
    void throwException(String message) throws ClusterException;
}

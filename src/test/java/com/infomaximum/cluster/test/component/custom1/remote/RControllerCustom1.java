package com.infomaximum.cluster.test.component.custom1.remote;

import com.infomaximum.cluster.core.remote.struct.ClusterInputStream;
import com.infomaximum.cluster.core.remote.struct.RController;

public interface RControllerCustom1 extends RController {

    String empty();
    ClusterInputStream getInputStream(int size);
}

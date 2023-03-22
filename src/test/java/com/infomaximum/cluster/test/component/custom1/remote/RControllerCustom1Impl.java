package com.infomaximum.cluster.test.component.custom1.remote;


import com.infomaximum.cluster.core.remote.AbstractRController;
import com.infomaximum.cluster.core.remote.struct.ClusterInputStream;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;

import java.io.IOException;
import java.io.InputStream;

public class RControllerCustom1Impl extends AbstractRController<Custom1Component> implements RControllerCustom1 {

    private RControllerCustom1Impl(Custom1Component component) {
        super(component);
    }

    @Override
    public String empty() {
        return null;
    }

    @Override
    public ClusterInputStream getInputStream(int size) {
        final int[] count = {0};
        return new ClusterInputStream(new InputStream() {
            @Override
            public int read() {
                if (count[0]>=size) {
                    return -1;
                }
                count[0]++;

                return count[0]%255;
            }
        });
    }
}

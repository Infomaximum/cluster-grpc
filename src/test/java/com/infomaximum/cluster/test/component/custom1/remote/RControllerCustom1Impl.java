package com.infomaximum.cluster.test.component.custom1.remote;

import com.infomaximum.cluster.core.remote.AbstractRController;
import com.infomaximum.cluster.core.remote.struct.ClusterInputStream;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.test.component.custom1.Custom1Component;
import com.infomaximum.cluster.utils.RandomUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class RControllerCustom1Impl extends AbstractRController<Custom1Component> implements RControllerCustom1 {

    public static final String HEAVY_STR;
    public static final String HEAVY_ARG;

    static {
        byte[] bytes = new byte[5 * 1024 * 1024];
        RandomUtil.random.nextBytes(bytes);
        HEAVY_STR = new String(bytes, StandardCharsets.UTF_8);

        bytes = new byte[5 * 1024 * 1024];
        RandomUtil.random.nextBytes(bytes);
        HEAVY_ARG = new String(bytes, StandardCharsets.UTF_8);
    }

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

    @Override
    public String slowRequest(long time) throws ClusterException {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "OK";
    }

    @Override
    public String heavyArgRequest(String arg) throws ClusterException {
        if (HEAVY_ARG.equals(arg)) {
            return "OK";
        }
        return HEAVY_STR;
    }

    @Override
    public String sumArg(String arg1, String arg2, String arg3) throws ClusterException {
        return arg1 + arg2 + arg3;
    }

    @Override
    public void throwException(String message) throws ClusterException {
        throw new ClusterException(message);
    }
}

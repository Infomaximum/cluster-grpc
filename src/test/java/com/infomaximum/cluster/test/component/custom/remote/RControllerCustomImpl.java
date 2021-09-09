package com.infomaximum.cluster.test.component.custom.remote;


import com.infomaximum.cluster.core.remote.AbstractRController;
import com.infomaximum.cluster.test.component.custom.CustomComponent;

public class RControllerCustomImpl extends AbstractRController<CustomComponent> implements RControllerCustom {

    private RControllerCustomImpl(CustomComponent component) {
        super(component);
    }

    @Override
    public String empty() {
        return null;
    }
}

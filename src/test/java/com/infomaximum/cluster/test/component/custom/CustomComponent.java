package com.infomaximum.cluster.test.component.custom;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.struct.Component;

/**
 * Created by kris on 12.09.17.
 */
@com.infomaximum.cluster.anotation.Component(uuid = "com.infomaximum.cluster.test.component.custom")
public class CustomComponent extends Component {

    public CustomComponent(Cluster cluster) {
        super(cluster);
    }

}

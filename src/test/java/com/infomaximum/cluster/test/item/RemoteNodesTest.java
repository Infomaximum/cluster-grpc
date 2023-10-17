package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.test.Clusters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RemoteNodesTest {

    private final static Logger log = LoggerFactory.getLogger(RemoteNodesTest.class);

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void test(int modeId) {
        try (Clusters clusters = new Clusters.Builder(modeId)
                .build()
        ) {
            List<Node> remoteNodes1 = clusters.getCluster1().getRemoteNodes();
            Assertions.assertEquals(1, remoteNodes1.size());
            Assertions.assertEquals("node2", remoteNodes1.get(0).getName());

            List<Node> remoteNodes2 = clusters.getCluster2().getRemoteNodes();
            Assertions.assertEquals(1, remoteNodes2.size());
            Assertions.assertEquals("node1", remoteNodes2.get(0).getName());
        }
    }

}

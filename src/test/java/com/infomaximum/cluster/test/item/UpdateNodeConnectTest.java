package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.Node;
import com.infomaximum.cluster.event.CauseNodeDisconnect;
import com.infomaximum.cluster.event.UpdateNodeConnect;
import com.infomaximum.cluster.test.Clusters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UpdateNodeConnectTest {

    private final static Logger log = LoggerFactory.getLogger(UpdateNodeConnectTest.class);

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    public void test(int modeId) {
        ListenerUpdateConnect listenerUpdateConnect1 = new ListenerUpdateConnect();
        ListenerUpdateConnect listenerUpdateConnect2 = new ListenerUpdateConnect();
        try (Clusters clusters = new Clusters.Builder(modeId)
                .withListenerUpdateConnect(listenerUpdateConnect1, listenerUpdateConnect2)
                .build()
        ) {

        }

        List<String> events1 = listenerUpdateConnect1.getEvents();
        List<String> events2 = listenerUpdateConnect2.getEvents();
        log.debug("events1: {}, events2: {}", events1, events2);

        //Проверяем, что у всех есть событие подключения удаленной ноды
        Assertions.assertTrue(events1.contains("c-node2"));
        Assertions.assertTrue(events2.contains("c-node1"));

        //Проверяем, что у всех есть событие отключение удаленной ноды
        Assertions.assertTrue(events1.contains("d-node2"));
        Assertions.assertTrue(events2.contains("d-node1"));

        //Проверяем, что нет лишних событий
        Assertions.assertTrue(events1.size() == 2);
        Assertions.assertTrue(events2.size() == 2);
    }

    public class ListenerUpdateConnect implements UpdateNodeConnect {

        private List<String> events;

        public ListenerUpdateConnect() {
            events = new ArrayList<>();
        }

        public List<String> getEvents() {
            return events;
        }

        @Override
        public void onConnect(Node node) {
            events.add("c-" + node.getName());
        }

        @Override
        public void onDisconnect(Node node, CauseNodeDisconnect cause) {
            events.add("d-" + node.getName());
        }
    }
}

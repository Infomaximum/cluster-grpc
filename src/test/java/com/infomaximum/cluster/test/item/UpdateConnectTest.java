package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.core.service.transport.network.grpc.UpdateConnect;
import com.infomaximum.cluster.test.Clusters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UpdateConnectTest {

    private final static Logger log = LoggerFactory.getLogger(UpdateConnectTest.class);

    @Test
    public void test() {
        ListenerUpdateConnect listenerUpdateConnect = new ListenerUpdateConnect();
        try (Clusters clusters = new Clusters.Builder()
                .withListenerUpdateConnect(listenerUpdateConnect)
                .build()
        ) {

        }

        List<String> events = listenerUpdateConnect.getEvents();
        log.debug("events: " + events);
        events.sort(String::compareTo);
        String[] actual = events.toArray(new String[events.size()]);

        Assertions.assertArrayEquals(actual, new String[]{"c12", "c21","d12", "d21"});
    }

    public class ListenerUpdateConnect implements UpdateConnect {

        private List<String> events;

        public ListenerUpdateConnect() {
            events = new ArrayList<>();
        }

        @Override
        public void onConnect(byte source, byte target) {
            events.add("c" + String.valueOf(source) + String.valueOf(target));
        }

        @Override
        public void onDisconnect(byte source, byte target) {
            events.add("d" + String.valueOf(source) + String.valueOf(target));
        }

        public List<String> getEvents() {
            return events;
        }
    }
}

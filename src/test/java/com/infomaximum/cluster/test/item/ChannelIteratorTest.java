package com.infomaximum.cluster.test.item;

import com.infomaximum.cluster.core.remote.struct.ClusterInputStream;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.Channel;
import com.infomaximum.cluster.core.service.transport.network.grpc.internal.channel.utils.ChannelIterator;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

public class ChannelIteratorTest {

    @Test
    public void testEmpty1() {
        Map<UUID, List<Channel>> channelItems = new HashMap<>();
        ChannelIterator channelIterator = new ChannelIterator(channelItems);
        Assertions.assertFalse(channelIterator.hasNext());
    }

    @Test
    public void testEmpty2() {
        Map<UUID, List<Channel>> channelItems = new HashMap<>() {{
            put(UUID.randomUUID(), Lists.emptyList());
        }};
        ChannelIterator channelIterator = new ChannelIterator(channelItems);
        Assertions.assertFalse(channelIterator.hasNext());
    }

    @Test
    public void testEmpty3() {
        Map<UUID, List<Channel>> channelItems = new HashMap<>() {{
            put(UUID.randomUUID(), Lists.emptyList());
            put(UUID.randomUUID(), Lists.emptyList());
            put(UUID.randomUUID(), Lists.emptyList());
        }};
        ChannelIterator channelIterator = new ChannelIterator(channelItems);
        Assertions.assertFalse(channelIterator.hasNext());
    }

    @Test
    public void test1() {
        Channel channel1 = Mockito.spy(Channel.class);
        Mockito.when(channel1.isAvailable()).thenReturn(true);

        Map<UUID, List<Channel>> channelItems = new LinkedHashMap<>() {{
            put(UUID.randomUUID(), Lists.list(channel1));
        }};
        ChannelIterator channelIterator = new ChannelIterator(channelItems);

        Assertions.assertTrue(channelIterator.hasNext());
        Assertions.assertEquals(channel1, channelIterator.next());
        Assertions.assertFalse(channelIterator.hasNext());
    }

    @Test
    public void test2() {
        Channel channel1 = Mockito.spy(Channel.class);
        Mockito.when(channel1.isAvailable()).thenReturn(true);
        Channel channel2 = Mockito.spy(Channel.class);
        Mockito.when(channel2.isAvailable()).thenReturn(true);
        Channel channel3 = Mockito.spy(Channel.class);
        Mockito.when(channel3.isAvailable()).thenReturn(true);

        Map<UUID, List<Channel>> channelItems = new LinkedHashMap<>() {{
            put(UUID.randomUUID(), Lists.list(channel1));
            put(UUID.randomUUID(), Lists.list(channel2));
            put(UUID.randomUUID(), Lists.list(channel3));
        }};
        ChannelIterator channelIterator = new ChannelIterator(channelItems);

        Assertions.assertTrue(channelIterator.hasNext());
        Assertions.assertEquals(channel1, channelIterator.next());
        Assertions.assertTrue(channelIterator.hasNext());
        Assertions.assertEquals(channel2, channelIterator.next());
        Assertions.assertTrue(channelIterator.hasNext());
        Assertions.assertEquals(channel3, channelIterator.next());
        Assertions.assertFalse(channelIterator.hasNext());
    }

    @Test
    public void test3() {
        Channel channel1 = Mockito.spy(Channel.class);
        Mockito.when(channel1.isAvailable()).thenReturn(true);
        Channel channel2 = Mockito.spy(Channel.class);
        Mockito.when(channel2.isAvailable()).thenReturn(true);
        Channel channel3 = Mockito.spy(Channel.class);
        Mockito.when(channel3.isAvailable()).thenReturn(true);

        Map<UUID, List<Channel>> channelItems = new LinkedHashMap<>() {{
            put(UUID.randomUUID(), Lists.list(channel1, channel2, channel3));
        }};
        ChannelIterator channelIterator = new ChannelIterator(channelItems);

        Assertions.assertTrue(channelIterator.hasNext());
        Assertions.assertEquals(channel1, channelIterator.next());
        Assertions.assertTrue(channelIterator.hasNext());
        Assertions.assertEquals(channel2, channelIterator.next());
        Assertions.assertTrue(channelIterator.hasNext());
        Assertions.assertEquals(channel3, channelIterator.next());
        Assertions.assertFalse(channelIterator.hasNext());
    }

    @Test
    public void test5() {
        Channel channel1 = Mockito.spy(Channel.class);
        Mockito.when(channel1.isAvailable()).thenReturn(true);
        Channel channel2 = Mockito.spy(Channel.class);
        Mockito.when(channel2.isAvailable()).thenReturn(false);//Один канал не валиден
        Channel channel3 = Mockito.spy(Channel.class);
        Mockito.when(channel3.isAvailable()).thenReturn(true);

        Map<UUID, List<Channel>> channelItems = new LinkedHashMap<>() {{
            put(UUID.randomUUID(), Lists.list(channel1));
            put(UUID.randomUUID(), Lists.list(channel2));
            put(UUID.randomUUID(), Lists.list(channel3));
        }};
        ChannelIterator channelIterator = new ChannelIterator(channelItems);

        Assertions.assertTrue(channelIterator.hasNext());
        Assertions.assertEquals(channel1, channelIterator.next());
        Assertions.assertTrue(channelIterator.hasNext());
        Assertions.assertEquals(channel3, channelIterator.next());
        Assertions.assertFalse(channelIterator.hasNext());
    }

    @Test
    public void test6() {
        Channel channel1 = Mockito.spy(Channel.class);
        Mockito.when(channel1.isAvailable()).thenReturn(true);
        Channel channel2 = Mockito.spy(Channel.class);
        Mockito.when(channel2.isAvailable()).thenReturn(true);
        Channel channel3 = Mockito.spy(Channel.class);
        Mockito.when(channel3.isAvailable()).thenReturn(true);

        Map<UUID, List<Channel>> channelItems = new LinkedHashMap<>() {{
            put(UUID.randomUUID(), Lists.emptyList());
            put(UUID.randomUUID(), Lists.list(channel1));
            put(UUID.randomUUID(), Lists.emptyList());
            put(UUID.randomUUID(), Lists.emptyList());
            put(UUID.randomUUID(), Lists.list(channel2, channel3));
            put(UUID.randomUUID(), Lists.emptyList());
            put(UUID.randomUUID(), Lists.emptyList());
        }};
        ChannelIterator channelIterator = new ChannelIterator(channelItems);

        Assertions.assertTrue(channelIterator.hasNext());
        Assertions.assertEquals(channel1, channelIterator.next());
        Assertions.assertTrue(channelIterator.hasNext());
        Assertions.assertEquals(channel2, channelIterator.next());
        Assertions.assertTrue(channelIterator.hasNext());
        Assertions.assertEquals(channel3, channelIterator.next());
        Assertions.assertFalse(channelIterator.hasNext());
    }
}

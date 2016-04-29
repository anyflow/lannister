package net.anyflow.lannister.session;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hazelcast.core.IMap;

import io.netty.channel.ChannelId;
import net.anyflow.lannister.Repository;
import net.anyflow.lannister.topic.Topic;

public class Sessions {

    @SuppressWarnings("unused")
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Sessions.class);

    private final Map<ChannelId, Session> channelIdMap;
    private final IMap<String, Session> clientIdMap;

    protected Sessions() {
        channelIdMap = Maps.newHashMap();
        clientIdMap = Repository.SELF.generator().getMap("sessions");
    }

    protected void put(Session session) {
        synchronized (this) {
            channelIdMap.put(session.channelId(), session);
            clientIdMap.put(session.clientId(), session); // [MQTT-3.1.2-4]
        }
    }

    protected void remove(Session session) {
        synchronized (this) {
            channelIdMap.remove(session.channelId());

            if (session.isCleanSession()) {
                clientIdMap.remove(session.clientId());
            }
        }
    }

    public ImmutableMap<ChannelId, Session> channelIdMap() {
        return ImmutableMap.copyOf(channelIdMap);
    }

    private IMap<String, Session> mutableClientIdMap() {
        return clientIdMap;
    }

    protected ImmutableMap<String, Session> clientIdMap() {
        return ImmutableMap.copyOf(mutableClientIdMap());
    }

    protected void topicAdded(Topic topic) {
        mutableClientIdMap().values().stream().parallel()
                .filter(session -> session.topicSubscriptions().values().stream()
                        .anyMatch(ts -> ts.isMatch(topic.name())))
                .forEach(session -> topic.addSubscriber(session.clientId(), false));

        // TODO handling persist
    }
}
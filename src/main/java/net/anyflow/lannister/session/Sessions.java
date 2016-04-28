package net.anyflow.lannister.session;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hazelcast.core.IMap;

import io.netty.channel.ChannelId;
import net.anyflow.lannister.Repository;
import net.anyflow.lannister.topic.Topic;

public class Sessions {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Sessions.class);

	private final Map<String, Session> clientIdMap;
	private final Map<ChannelId, Session> channelIdMap;

	private final IMap<String, Session> persistedClientIdMap;

	protected Sessions() {
		clientIdMap = Maps.newHashMap();
		channelIdMap = Maps.newHashMap();

		persistedClientIdMap = Repository.SELF.generator().getMap("sessions");
	}

	protected Session getByClientId(String clientId, boolean includePersisted) {
		Session ret = clientIdMap.get(clientId);

		if (ret == null) {
			if (includePersisted == false) { return ret; }

			return persistedClientIdMap.get(clientId);
		}
		else {
			return ret;
		}
	}

	protected Session getByChannelId(ChannelId channelId) {
		return channelIdMap.get(channelId);
	}

	protected void put(Session session) {
		synchronized (this) {
			clientIdMap.put(session.clientId(), session);
			channelIdMap.put(session.channelId(), session);

			if (session.isCleanSession()) { return; }

			persistedClientIdMap.put(session.clientId(), session); // [MQTT-3.1.2-4]
		}
	}

	protected void remove(Session session, boolean includePersisted) {
		synchronized (this) {
			clientIdMap.remove(session.clientId());
			channelIdMap.remove(session.channelId());
		}

		if (includePersisted) {
			persistedClientIdMap.remove(session.clientId());
		}
	}

	private Map<String, Session> mutableClientIdMap(boolean includePersisted) {
		if (includePersisted == false) { return clientIdMap; }

		Map<String, Session> ret = Maps.newHashMap(clientIdMap);

		persistedClientIdMap.values().stream().forEach(session -> {
			ret.putIfAbsent(session.clientId(), session);
		});

		return ret;
	}

	protected ImmutableMap<String, Session> clientIdMap(boolean includePersisted) {
		return ImmutableMap.copyOf(mutableClientIdMap(includePersisted));
	}

	protected ImmutableMap<String, Session> persistedClientIdMap() {
		return ImmutableMap.copyOf(persistedClientIdMap);
	}

	protected Session persist(Session session) {
		if (session == null) {
			logger.error("Null session tried to be persisted.");
			return null;
		}

		return persistedClientIdMap.put(session.clientId(), session);
	}

	protected void topicAdded(Topic topic) {
		mutableClientIdMap(true).values().stream().parallel()
				.filter(session -> session.topicSubscriptions().values().stream()
						.anyMatch(ts -> ts.isMatch(topic.name())))
				.forEach(session -> topic.addSubscriber(session.clientId(), false));

		// TODO handling persist
	}
}
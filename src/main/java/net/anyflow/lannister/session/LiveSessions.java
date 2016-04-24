package net.anyflow.lannister.session;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import io.netty.channel.ChannelId;

public class LiveSessions {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LiveSessions.class);

	public static LiveSessions SELF;

	static {
		SELF = new LiveSessions();
	}

	private final Map<String, Session> clientIdMap;
	private final Map<ChannelId, Session> channelIdMap;

	private LiveSessions() {
		clientIdMap = Maps.newHashMap();
		channelIdMap = Maps.newHashMap();
	}

	public Session getByClientId(String clientId) {
		return clientIdMap.get(clientId);
	}

	public Session getByChannelId(ChannelId channelId) {
		return channelIdMap.get(channelId);
	}

	public void put(Session session) {
		synchronized (this) {
			clientIdMap.put(session.clientId(), session);
			channelIdMap.put(session.ctx().channel().id(), session);

			Repository.SELF.clientIdSessionMap().put(session.clientId(), session); // [MQTT-3.1.2-4]
		}
	}

	public void dispose(Session session, boolean sendWill) {
		synchronized (this) {
			clientIdMap.remove(session.clientId());
			channelIdMap.remove(session.ctx().channel().id());
		}

		session.dispose(sendWill);
	}

	public ImmutableMap<String, Session> clientIdMap() {
		return ImmutableMap.copyOf(clientIdMap);
	}
}
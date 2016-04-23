package net.anyflow.lannister.session;

import java.util.Collection;
import java.util.Map;

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
	private final Map<ChannelId, Session> channelMap;

	private LiveSessions() {
		clientIdMap = Maps.newHashMap();
		channelMap = Maps.newHashMap();
	}

	public Session getByClientId(String clientId) {
		return clientIdMap.get(clientId);
	}

	public Session getByChannelId(ChannelId channelId) {
		return channelMap.get(channelId);
	}

	public void put(Session session) {
		synchronized (this) {
			clientIdMap.put(session.clientId(), session);
			channelMap.put(session.ctx().channel().id(), session);

			Repository.SELF.sessions().put(session.clientId(), session); // [MQTT-3.1.2-4]
		}
	}

	public void dispose(Session session, boolean sendWill) {
		synchronized (this) {
			clientIdMap.remove(session.clientId());
			channelMap.remove(session.ctx().channel().id());
		}

		session.dispose(sendWill);
	}

	public Collection<Session> list() {
		return clientIdMap.values();
	}
}
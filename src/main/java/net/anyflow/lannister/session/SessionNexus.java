package net.anyflow.lannister.session;

import java.util.Map;

import com.google.common.collect.Maps;

public class SessionNexus {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SessionNexus.class);

	public static SessionNexus SELF;

	static {
		SELF = new SessionNexus();
	}

	private final Map<String, Session> clientIdMap;
	private final Map<String, Session> channelMap;
	private final Object locker;

	private SessionNexus() {
		clientIdMap = Maps.newHashMap();
		channelMap = Maps.newHashMap();

		locker = new Object();
	}

	public Session getByClientId(String clientId) {
		return clientIdMap.get(clientId);
	}

	public Session getByChannelId(String channelId) {
		return channelMap.get(channelId);
	}

	public void put(Session session) {
		synchronized (locker) {
			clientIdMap.put(session.clientId(), session);
			channelMap.put(session.ctx().channel().id().toString(), session);
		}
	}
}
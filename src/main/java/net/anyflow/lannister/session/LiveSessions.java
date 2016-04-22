package net.anyflow.lannister.session;

import java.util.Map;

import com.google.common.collect.Maps;

import io.netty.channel.ChannelHandlerContext;

public class LiveSessions {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LiveSessions.class);

	public static LiveSessions SELF;

	static {
		SELF = new LiveSessions();
	}

	private final Map<String, Session> clientIdMap;
	private final Map<String, Session> channelMap;

	private LiveSessions() {
		clientIdMap = Maps.newHashMap();
		channelMap = Maps.newHashMap();
	}

	public Session getByClientId(String clientId) {
		return clientIdMap.get(clientId);
	}

	public Session getByChannelId(String channelId) {
		return channelMap.get(channelId);
	}

	public void put(Session session) {
		synchronized (this) {
			clientIdMap.put(session.clientId(), session);
			channelMap.put(session.ctx().channel().id().toString(), session);
		}
	}

	public void dispose(ChannelHandlerContext ctx) {
		Session session = getByChannelId(ctx.channel().id().toString());
		if (session == null) { return; }

		dispose(session);
	}

	public void dispose(Session session) {
		logger.debug("SessionNexus.dispose() called : sessionID={}", session.id());

		session.dispose();

		synchronized (this) {
			clientIdMap.remove(session.clientId());
			channelMap.remove(session.ctx().channel().id().toString());
		}
	}
}
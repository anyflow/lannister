package net.anyflow.lannister.session;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import io.netty.channel.ChannelId;

public class Sessions {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Sessions.class);

	public static Sessions SELF;

	static {
		SELF = new Sessions();
	}

	private final Map<String, Session> clientIdMap;
	private final Map<ChannelId, Session> channelIdMap;

	private Sessions() {
		clientIdMap = Maps.newHashMap();
		channelIdMap = Maps.newHashMap();
	}

	public Session getByClientId(String clientId, boolean includePersisted) {
		Session ret = clientIdMap.get(clientId);

		if (ret == null) {
			if (includePersisted == false) { return ret; }

			return Repository.SELF.clientIdSessionMap().get(clientId);
		}
		else {
			return ret;
		}
	}

	public Session getByChannelId(ChannelId channelId) {
		return channelIdMap.get(channelId);
	}

	public void put(Session session) {
		synchronized (this) {
			clientIdMap.put(session.clientId(), session);
			channelIdMap.put(session.ctx().channel().id(), session);

			if (session.cleanSession()) { return; }

			Repository.SELF.clientIdSessionMap().put(session.clientId(), session); // [MQTT-3.1.2-4]
		}
	}

	public void dispose(Session session, boolean sendWill) {
		synchronized (this) {
			clientIdMap.remove(session.clientId());
			channelIdMap.remove(session.ctx().channel().id());
		}

		if (sendWill == false) {
			// TODO Check whether DISCONNECT means delete persistent session.
			Repository.SELF.clientIdSessionMap().remove(session.clientId());
		}

		session.dispose(sendWill);
	}

	public ImmutableMap<String, Session> clientIdMap() {
		return ImmutableMap.copyOf(clientIdMap);
	}
}
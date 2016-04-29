package net.anyflow.lannister.session;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import io.netty.channel.ChannelId;
import net.anyflow.lannister.Repository;
import net.anyflow.lannister.topic.Notification;
import net.anyflow.lannister.topic.Topic;

public class Sessions implements MessageListener<Notification> {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Sessions.class);

	private final Map<ChannelId, Session> lives;
	private final IMap<String, Session> all;

	protected Sessions() {
		lives = Maps.newHashMap();
		all = Repository.SELF.generator().getMap("sessions");
	}

	public void put(Session session) {
		synchronized (this) {
			lives.put(session.channelId(), session);
			all.put(session.clientId(), session); // [MQTT-3.1.2-4]
		}
	}

	protected void remove(Session session) {
		synchronized (this) {
			lives.remove(session.channelId());

			if (session.isCleanSession()) {
				all.remove(session.clientId());
			}
		}
	}

	public ImmutableMap<ChannelId, Session> lives() {
		return ImmutableMap.copyOf(lives);
	}

	public ImmutableMap<String, Session> all() {
		return ImmutableMap.copyOf(all);
	}

	public void topicAdded(Topic topic) {
		all.values().stream().parallel()
				.filter(session -> session.topicSubscriptions().values().stream()
						.anyMatch(ts -> ts.isMatch(topic.name())))
				.forEach(session -> topic.addSubscriber(session.clientId()));
	}

	@Override
	public void onMessage(Message<Notification> message) {
		Notification notified = message.getMessageObject();

		Session session = lives.get(notified.clientId());
		if (session == null) { return; }

		session.onPublish(notified.topic(), notified.message());
	}
}
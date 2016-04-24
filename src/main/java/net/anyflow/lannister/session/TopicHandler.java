package net.anyflow.lannister.session;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import com.google.common.base.Strings;

import io.netty.handler.codec.mqtt.MqttQoS;

public class TopicHandler {

	private final Map<String, Topic> topics;
	private final Map<Integer, Message> messages;
	private final Synchronizer synchronizer;

	public static boolean isATopicName(String topicName) {
		return Strings.isNullOrEmpty(topicName) == false;
	}

	public TopicHandler(Map<String, Topic> topics, Map<Integer, Message> messages, Synchronizer synchronizer) {
		this.topics = topics;
		this.messages = messages;
		this.synchronizer = synchronizer;
	}

	public boolean putTopic(String topicName, MqttQoS mqttQoS, Session session) {
		if (isATopicName(topicName) == false) { return false; }

		String registrationId = Repository.SELF.broadcaster(topicName).addMessageListener(session);

		Topic topic = new Topic(registrationId, topicName, mqttQoS);

		Topic old = topics.get(topicName);
		if (old != null) {
			topic.setRetainedMessage(old.retainedMessage());
		}

		topics.put(topicName, topic); // [MQTT-3.8.4-3]

		synchronizer.execute();

		return true;
	}

	public Topic removeTopic(final String topicName) {
		if (isATopicName(topicName) == false) { return null; }

		Topic ret = topics.remove(topicName);
		if (ret == null) { return null; }

		messages.entrySet().removeIf(new Predicate<Entry<Integer, Message>>() {
			@Override
			public boolean test(Entry<Integer, Message> t) {
				if (topicName.equals(t.getValue().topicName()) == false) { return false; }
				if (t.getValue().isSent()) { return false; } // [MQTT-3.10.4-3]

				return true;
			}
		});

		Repository.SELF.broadcaster(topicName).removeMessageListener(ret.registrationId());

		synchronizer.execute();

		return ret;
	}
}

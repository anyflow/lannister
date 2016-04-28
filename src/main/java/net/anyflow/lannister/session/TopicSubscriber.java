package net.anyflow.lannister.session;

import java.util.Map;
import java.util.function.Predicate;

public class TopicSubscriber {

	private final Map<String, TopicSubscription> topicSubscriptions;
	private final Map<Integer, Message> messages;
	private final Synchronizer synchronizer;
	private final String clientId;

	protected TopicSubscriber(String clientId, Map<String, TopicSubscription> topicSubscriptions,
			Map<Integer, Message> messages, Synchronizer synchronizer) {
		this.clientId = clientId;
		this.topicSubscriptions = topicSubscriptions;
		this.messages = messages;
		this.synchronizer = synchronizer;
	}

	protected TopicSubscription[] matches(String topicName) {
		return topicSubscriptions.values().stream().filter(new Predicate<TopicSubscription>() {
			@Override
			public boolean test(TopicSubscription t) {
				return t.isMatch(topicName);
			}
		}).toArray(TopicSubscription[]::new);
	}

	protected TopicSubscription putTopicSubscription(TopicSubscription topicSubscription) {
		TopicSubscription ret = topicSubscriptions.put(topicSubscription.topicFilter(), topicSubscription); // [MQTT-3.8.4-3]

		synchronizer.execute();

		return ret;
	}

	protected TopicSubscription removeTopicSubscription(final String topicFilter) {
		TopicSubscription ret = topicSubscriptions.remove(topicFilter);
		if (ret == null) { return null; }

		Topic.removeSubscribers(topicFilter, clientId, true);

		messages.entrySet().removeIf(entry -> {
			if (ret.isMatch(entry.getValue().topicName()) == false) { return false; }
			if (entry.getValue().isSent()) { return false; } // [MQTT-3.10.4-3]

			return true;
		});

		synchronizer.execute();

		return ret;
	}
}
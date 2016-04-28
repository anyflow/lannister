package net.anyflow.lannister.topic;

import java.util.Map;
import java.util.function.Predicate;

public class TopicSubscriptionHandler {

	private final Map<String, TopicSubscription> topicSubscriptions;
	private final String clientId;

	public TopicSubscriptionHandler(String clientId, Map<String, TopicSubscription> topicSubscriptions) {
		this.clientId = clientId;
		this.topicSubscriptions = topicSubscriptions;
	}

	public TopicSubscription[] matches(String topicName) {
		return topicSubscriptions.values().stream().filter(new Predicate<TopicSubscription>() {
			@Override
			public boolean test(TopicSubscription t) {
				return t.isMatch(topicName);
			}
		}).toArray(TopicSubscription[]::new);
	}

	public TopicSubscription putTopicSubscription(TopicSubscription topicSubscription) {
		return topicSubscriptions.put(topicSubscription.topicFilter(), topicSubscription); // [MQTT-3.8.4-3]
	}

	public TopicSubscription removeTopicSubscription(final String topicFilter) {
		TopicSubscription ret = topicSubscriptions.remove(topicFilter);
		if (ret == null) { return null; }

		Topic.removeSubscribers(topicFilter, clientId, true);

		// TODO [MQTT-3.10.4-3] If a Server deletes a Subscription It MUST
		// complete the delivery of any QoS 1 or QoS 2 messages which it has
		// started to send to the Client.

		return ret;
	}
}
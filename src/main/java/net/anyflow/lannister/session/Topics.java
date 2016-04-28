package net.anyflow.lannister.session;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import com.hazelcast.core.IMap;

public class Topics {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Topics.class);

	private final IMap<String, Topic> topics;

	protected Topics() {
		this.topics = Repository.SELF.generator().getMap("topics");
	}

	protected Topic get(String topicName) {
		return topics.get(topicName);
	}

	protected Topic put(Topic topic) {
		if (topic == null) {
			logger.error("Null topic tried to be inserted.");
			return null;
		}

		return topics.put(topic.name(), topic);
	}

	private Topic[] matches(String topicFilter) {
		return topics.values().stream().filter(topic -> TopicSubscription.isMatch(topicFilter, topic.name()))
				.toArray(Topic[]::new);
	}

	protected Topic remove(Topic topic) {
		if (topic == null) {
			logger.error("Null topic tried to be removed.");
			return null;
		}

		return topics.remove(topic.name());
	}

	protected void removeSubscribers(String topicFilter, String clientId, boolean persist) {
		List<Topic> changed = Lists.newArrayList();

		Stream.of(matches(topicFilter)).forEach(t -> {
			t.removeSubscriber(clientId, true);
			changed.add(t);
		});

		changed.stream().forEach(t -> topics.put(t.name(), t)); // persist
	}
}
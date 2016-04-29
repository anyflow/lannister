package net.anyflow.lannister.topic;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import net.anyflow.lannister.Repository;
import net.anyflow.lannister.message.SentMessageStatus;
import net.anyflow.lannister.session.Session;

public class Topics {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Topics.class);

	private final IMap<String, Topic> topics;
	private final ITopic<Notification> notifier;

	protected Topics() {
		this.topics = Repository.SELF.generator().getMap("topics");
		this.notifier = Repository.SELF.generator().getTopic("publishNotifier");
		this.notifier.addMessageListener(Session.NEXUS);
	}

	protected ImmutableList<Topic> topics() {
		return ImmutableList.copyOf(topics.values());
	}

	public ITopic<Notification> notifier() {
		return notifier;
	}

	public Topic get(String name) {
		return topics.get(name);
	}

	protected Topic get(String clientId, int brokerMessageId) {
		return topics.values().stream().filter(t -> {
			TopicSubscriber ts = t.subscribers().get(clientId);

			return ts != null && ts.sentMessageStatuses().get(brokerMessageId) != null;
		}).findAny().orElse(null);
	}

	protected Topic put(Topic topic) {
		if (topic == null) {
			logger.error("Null topic tried to be inserted.");
			return null;
		}

		return topics.put(topic.name(), topic);
	}

	protected Topic remove(Topic topic) {
		if (topic == null) {
			logger.error("Null topic tried to be removed.");
			return null;
		}

		return topics.remove(topic.name());
	}

	public void removeSubscribers(String topicFilter, String clientId, boolean persist) {
		List<Topic> changed = Lists.newArrayList();

		Stream.of(matches(topicFilter)).forEach(t -> {
			t.removeSubscriber(clientId, true);
			changed.add(t);
		});

		changed.stream().forEach(t -> topics.put(t.name(), t)); // persist
	}

	private Topic[] matches(String topicFilter) {
		return topics.values().stream().filter(topic -> TopicSubscription.isMatch(topicFilter, topic.name()))
				.toArray(Topic[]::new);
	}

	public SentMessageStatus messageAcked(String clientId, int messageId) {
		Topic topic = get(clientId, messageId);
		if (topic == null) { return null; }

		return topic.subscribers().get(clientId).removeMessageStatus(messageId);
	}
}
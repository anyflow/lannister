package net.anyflow.lannister.session;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.hazelcast.core.IMap;

public class Topics {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Topics.class);

	private final IMap<String, Topic> topics;

	protected Topics() {
		this.topics = Repository.SELF.generator().getMap("topics");
	}

	protected ImmutableMap<String, Topic> topics() {
		return ImmutableMap.copyOf(topics);
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

	protected ImmutableList<Topic> matches(String topicFilter) {
		List<Topic> ret = Lists.newArrayList();

		topics.values().stream().filter(topic -> TopicSubscription.isMatch(topicFilter, topic.name()))
				.forEach(topic -> ret.add(topic));

		return ImmutableList.copyOf(ret);
	}

	protected Topic remove(Topic topic) {
		if (topic == null) {
			logger.error("Null topic tried to be removed.");
			return null;
		}

		return topics.remove(topic.name());
	}
}
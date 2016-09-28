/*
 * Copyright 2016 The Lannister Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.anyflow.lannister.topic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import net.anyflow.lannister.Hazelcast;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.session.Sessions;

public class Topics {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Topics.class);

	private final IMap<String, Topic> topics;
	private final ITopic<Notification> notifier;

	protected Topics(Sessions sessions) {
		this.topics = Hazelcast.INSTANCE.getMap("topics");
		this.notifier = Hazelcast.INSTANCE.getTopic("publishNotifier");
		this.notifier.addMessageListener(sessions);
	}

	public IMap<String, Topic> map() {
		return topics;
	}

	public ITopic<Notification> notifier() {
		return notifier;
	}

	public Topic get(String name) {
		return topics.get(name);
	}

	public enum ClientType {
		SUBSCRIBER,
		PUBLISHER
	}

	public Topic get(String clientId, int messageId, ClientType clientType) {
		switch (clientType) {
		case SUBSCRIBER:
			return getFromSubscriber(clientId, messageId);

		case PUBLISHER:
			return getFromPublisher(clientId, messageId);

		default:
			throw new IllegalArgumentException();
		}
	}

	private Topic getFromPublisher(String publisherId, int messageId) {
		return topics.values().parallelStream().filter(t -> t.getInboundMessageStatus(publisherId, messageId) != null)
				.findAny().orElse(null);
	}

	private Topic getFromSubscriber(String subscriberId, int messageId) {
		return topics.values().stream().filter(t -> {
			TopicSubscriber ts = t.subscribers().get(subscriberId);

			return ts != null && ts.outboundMessageStatuses().get(messageId) != null;
		}).findAny().orElse(null);
	}

	protected void persist(Topic topic) {
		assert topic != null;

		topics.set(topic.name(), topic);
	}

	public void insert(Topic topic) {
		assert topic != null;

		topic.updateSubscribers();

		// TODO should be added in case of no subscriber & no retained Message?
		persist(topic);
	}

	public Topic remove(Topic topic) {
		assert topic != null;
		topic.dispose();

		return topics.remove(topic.name());
	}

	public Topic prepare(Message message) {
		Topic topic = get(message.topicName());
		if (topic == null) {
			topic = new Topic(message.topicName());
			insert(topic);
		}

		return topic;
	}

	public List<Topic> matches(String topicFilter) {
		return topics.values().stream().filter(topic -> TopicMatcher.match(topicFilter, topic.name()))
				.collect(Collectors.toList());
	}

	public Collection<Topic> matches(Collection<String> topicFilters) {
		Map<String, Topic> ret = Maps.newHashMap();

		topics.values().stream().forEach(t -> {
			if (topicFilters.stream().filter(tf -> TopicMatcher.match(tf, t.name())).count() <= 0) { return; }
			if (ret.containsKey(t.name())) { return; }

			ret.put(t.name(), t);
		});

		return ret.values();
	}
}
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

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import net.anyflow.lannister.Repository;
import net.anyflow.lannister.session.Sessions;

public class Topics {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Topics.class);

	private final IMap<String, Topic> topics;
	private final ITopic<Notification> notifier;

	public Topics(Sessions sessions) {
		this.topics = Repository.SELF.generator().getMap("topics");
		this.notifier = Repository.SELF.generator().getTopic("publishNotifier");
		this.notifier.addMessageListener(sessions);
	}

	public ImmutableMap<String, Topic> map() {
		return ImmutableMap.copyOf(topics);
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

	public Topic get(String clientId, int brokerMessageId, ClientType clientType) {
		switch (clientType) {
		case SUBSCRIBER:
			return getFromSubscriber(clientId, brokerMessageId);

		case PUBLISHER:
			return getFromPublisher(clientId, brokerMessageId);

		default:
			throw new IllegalArgumentException();
		}
	}

	private Topic getFromPublisher(String publisherId, int publisherMessageId) {
		return topics.values().parallelStream()
				.filter(t -> t.getInboundMessageStatus(publisherId, publisherMessageId) != null).findAny().orElse(null);
	}

	private Topic getFromSubscriber(String subscriberId, int brokerMessageId) {
		return topics.values().stream().filter(t -> {
			TopicSubscriber ts = t.subscribers().get(subscriberId);

			return ts != null && ts.sentOutboundMessageStatuses().get(brokerMessageId) != null;
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

	public void removeSubscribers(String topicFilter, String clientId) {
		List<Topic> changed = Lists.newArrayList();

		Stream.of(matches(topicFilter)).forEach(t -> {
			t.removeSubscriber(clientId);
			changed.add(t);
		});

		changed.stream().forEach(t -> topics.put(t.name(), t)); // persist
	}

	private Topic[] matches(String topicFilter) {
		return topics.values().stream().filter(topic -> TopicSubscription.isMatch(topicFilter, topic.name()))
				.toArray(Topic[]::new);
	}
}
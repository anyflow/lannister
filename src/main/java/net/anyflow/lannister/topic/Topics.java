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

import com.google.common.collect.ImmutableMap;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import net.anyflow.lannister.Hazelcast;
import net.anyflow.lannister.session.Sessions;

public class Topics {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Topics.class);

	private final IMap<String, Topic> topics;
	private final ITopic<Notification> notifier;

	public Topics(Sessions sessions) {
		this.topics = Hazelcast.SELF.generator().getMap("topics");
		this.notifier = Hazelcast.SELF.generator().getTopic("publishNotifier");
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

		topic.dispose();

		return topics.remove(topic.name());
	}
}
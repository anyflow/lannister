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

package net.anyflow.lannister.session;

import com.hazelcast.map.MapInterceptor;

import io.netty.util.concurrent.GlobalEventExecutor;
import net.anyflow.lannister.plugin.ITopicSubscription;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicMatcher;
import net.anyflow.lannister.topic.TopicSubscriber;

public class TopicSubscriptionInterceptor implements MapInterceptor {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(TopicSubscriptionInterceptor.class);

	private static final long serialVersionUID = -7359310249499933518L;

	private String clientId;

	protected TopicSubscriptionInterceptor(String clientId) {
		this.clientId = clientId;
	}

	@Override
	public Object interceptGet(Object value) {
		return value;
	}

	@Override
	public void afterGet(Object value) {
		// Do nothing
	}

	@Override
	public Object interceptPut(Object oldValue, Object newValue) {
		return newValue;
	}

	@Override
	public Object interceptRemove(Object removedValue) {
		return removedValue;
	}

	@Override
	public void afterPut(Object value) {
		ITopicSubscription topicSubscription = (ITopicSubscription) value;

		GlobalEventExecutor.INSTANCE.submit(() -> {
			Topic.NEXUS.map().values().stream()
					.filter(t -> TopicMatcher.match(topicSubscription.topicFilter(), t.name()))
					.forEach(t -> t.subscribers().set(clientId, new TopicSubscriber(clientId, t.name())));
		});
	}

	@Override
	public void afterRemove(Object value) {
		ITopicSubscription topicSubscription = (ITopicSubscription) value;

		GlobalEventExecutor.INSTANCE.submit(() -> {
			Topic.NEXUS.map().values().stream()
					.filter(t -> TopicMatcher.match(topicSubscription.topicFilter(), t.name()))
					.forEach(t -> t.subscribers().remove(clientId));
		});
	}

	@Override
	public int hashCode() {
		return clientId.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) { return false; }

		return clientId.equals(o.toString());
	}
}
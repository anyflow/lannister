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

import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicMatcher;
import net.anyflow.lannister.topic.TopicSubscriber;
import net.anyflow.lannister.topic.TopicSubscription;

public class TopicSubscriptionInterceptor implements MapInterceptor {

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
	public void afterPut(Object value) {
		TopicSubscription topicSubscription = (TopicSubscription) value;

		Session.NEXUS.channelHandlerContext(clientId).executor().submit(() -> {
			Topic.NEXUS.map().values().stream()
					.filter(t -> TopicMatcher.match(topicSubscription.topicFilter(), t.name()))
					.forEach(t -> t.subscribers().put(clientId, new TopicSubscriber(clientId, t.name())));
		});
	}

	@Override
	public Object interceptRemove(Object removedValue) {
		TopicSubscription topicSubscription = (TopicSubscription) removedValue;

		Session.NEXUS.channelHandlerContext(clientId).executor().submit(() -> {
			Topic.NEXUS.map().values().stream()
					.filter(t -> TopicMatcher.match(topicSubscription.topicFilter(), t.name()))
					.forEach(t -> t.subscribers().remove(clientId));
		});

		return removedValue;
	}

	@Override
	public void afterRemove(Object value) {
		// Do nothing
	}

	@Override
	public int hashCode() {
		return clientId.hashCode();
	}
}
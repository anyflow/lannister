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

import com.hazelcast.map.MapInterceptor;

import io.netty.util.concurrent.GlobalEventExecutor;

public class TopicSubscriberInterceptor implements MapInterceptor {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TopicSubscriberInterceptor.class);

	private static final long serialVersionUID = -258619017472206696L;

	private String topicName;

	protected TopicSubscriberInterceptor(String topicName) {
		this.topicName = topicName;
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
		// DO NOTHING
	}

	@Override
	public void afterRemove(Object value) {
		GlobalEventExecutor.INSTANCE.submit(() -> {
			Topic topic = Topic.NEXUS.get(topicName);
			if (topicName.startsWith("$SYS") || topic == null || topic.subscribers().size() > 0) { return; }

			Topic.NEXUS.remove(topic);
		});
	}

	@Override
	public int hashCode() {
		return topicName.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) { return false; }

		return topicName.equals(o.toString());
	}
}
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
package net.anyflow.lannister.cluster;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.hazelcast.monitor.LocalTopicStats;

import io.netty.util.concurrent.GlobalEventExecutor;

public class SingleTopic<E> implements ITopic<E> {

	private final String name;
	private final Map<String, MessageListener<E>> messageListeners;

	public SingleTopic(String name) {
		this.name = name;
		this.messageListeners = Maps.newConcurrentMap();
	}

	@Override
	public void destroy() {
		// DO NOTHING
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void publish(E message) {
		final Date now = new Date();

		GlobalEventExecutor.INSTANCE.execute(() -> {
			Message<E> msg = new Message<E>(name, message, now.getTime(), null);
			messageListeners.values().forEach(c -> c.onMessage(msg));
		});
	}

	@Override
	public String addMessageListener(MessageListener<E> listener) {
		UUID ret = UUID.randomUUID();

		messageListeners.put(ret.toString(), listener);

		return ret.toString();
	}

	@Override
	public boolean removeMessageListener(String registrationId) {
		return messageListeners.remove(registrationId) != null;
	}

	@Override
	public String getPartitionKey() {
		throw new Error("The method should not be called");
	}

	@Override
	public String getServiceName() {
		throw new Error("The method should not be called");
	}

	@Override
	public LocalTopicStats getLocalTopicStats() {
		throw new Error("The method should not be called");
	}
}

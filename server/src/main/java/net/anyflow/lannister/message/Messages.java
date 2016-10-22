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
package net.anyflow.lannister.message;

import java.util.Set;

import com.google.common.collect.Sets;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;
import net.anyflow.lannister.message.InboundMessageStatus.Status;

public class Messages {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Messages.class);

	private final Map<String, Message> data;

	protected Messages() {
		data = ClusterDataFactory.INSTANCE.createMap("Messages_data");
	}

	public static String key(String publisherId, Integer messageId) {
		return publisherId + "_" + Integer.toString(messageId);
	}

	public int size() {
		return data.size();
	}

	public void dispose() {
		data.dispose();
	}

	public Message remove(String key) {
		Message ret = data.remove(key);
		if (ret == null) { return null; }

		if (logger.isDebugEnabled()) {
			logger.debug("REMOVEed Message [messageId={}, publisherId={}, topicName=]", ret.id(), ret.publisherId(),
					ret.topicName());
			logger.debug("Messages size={}", data.size());
		}

		return ret;
	}

	public Set<String> keySet() {
		return Sets.newHashSet(data.keySet());
	}

	public Message get(String key) {
		return data.get(key);
	}

	public void put(String key, Message message) {
		data.put(key, message);

		InboundMessageStatus.NEXUS.put(new InboundMessageStatus(message.key(), message.publisherId(), message.id(),
				message.topicName(), Status.RECEIVED));

		if (logger.isDebugEnabled()) {
			logger.debug("ADDed Message [messageId={}, publisherId={}, topicName=]", message.id(),
					message.publisherId(), message.topicName());
			logger.debug("Messages size={}", data.size());
		}
	}
}

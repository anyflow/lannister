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

import com.google.common.collect.Maps;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;

public class MessageReferenceCounts {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageReferenceCounts.class);

	public static final MessageReferenceCounts INSTANCE = new MessageReferenceCounts();

	private final Map<String, Integer> data; // key : message.key()

	private MessageReferenceCounts() {
		this.data = ClusterDataFactory.INSTANCE.createMap("MessagesReferenceCounts_data");
	}

	public java.util.Map<String, Integer> data() {
		java.util.Map<String, Integer> ret = Maps.newHashMap();
		data.keySet().stream().forEach(k -> ret.put(k, data.get(k)));

		return ret;
	}

	public void retain(String messageKey) {
		Integer count = data.get(messageKey);
		if (count == null) {
			count = 0;
		}

		data.put(messageKey, ++count);
		logger.debug("RETAINed message reference [count={}, messageKey={}]", count, messageKey);
	}

	public void release(String messageKey) {
		Integer count = data.get(messageKey);
		if (count == null) { return; }

		if (count <= 0) {
			logger.error("Invalid Message reference Found![key={}, count={}]", messageKey, count);
			return;
		}
		else if (count == 1) {
			data.remove(messageKey);
			logger.debug("REMOVEed Message reference [key={}]", messageKey);

			Message.NEXUS.remove(messageKey);
		}
		else {
			data.put(messageKey, --count);
			logger.debug("RELEASEed Message reference [key={}, count={}]", messageKey, count);
		}
	}

	public int size() {
		return data.size();
	}
}

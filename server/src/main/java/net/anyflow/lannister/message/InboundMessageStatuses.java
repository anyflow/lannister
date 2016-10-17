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
import java.util.concurrent.locks.Lock;

import com.google.common.collect.Sets;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;
import net.anyflow.lannister.cluster.SerializableIntegerSet;
import net.anyflow.lannister.cluster.SerializableStringSet;

public class InboundMessageStatuses {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InboundMessageStatuses.class);

	private final Map<String, InboundMessageStatus> data;
	private final Map<Integer, SerializableStringSet> messageidIndex;
	private final Map<String, SerializableIntegerSet> clientidIndex;

	private final Lock modifyLock;

	protected InboundMessageStatuses() {
		this.data = ClusterDataFactory.INSTANCE.createMap("InboundMessageStatuses_data");
		this.messageidIndex = ClusterDataFactory.INSTANCE.createMap("InboundMessageStatuses_messageidIndex");
		this.clientidIndex = ClusterDataFactory.INSTANCE.createMap("InboundMessageStatuses_clientidIndex");

		this.modifyLock = ClusterDataFactory.INSTANCE.createLock("InboundMessageStatuses_modifyLock");
	}

	public static String key(Integer messageId, String clientId) {
		return clientId + "_" + Integer.toString(messageId);
	}

	public Set<String> keySet() {
		return Sets.newHashSet(data.keySet());
	}

	public void put(InboundMessageStatus inboundMessageStatus) {
		if (inboundMessageStatus == null) { return; }

		modifyLock.lock();
		try {
			this.data.put(inboundMessageStatus.key(), inboundMessageStatus);

			SerializableStringSet clientIds = this.messageidIndex.get(inboundMessageStatus.messageId());
			if (clientIds == null) {
				clientIds = new SerializableStringSet();
			}
			clientIds.add(inboundMessageStatus.clientId());
			this.messageidIndex.put(inboundMessageStatus.messageId(), clientIds);

			SerializableIntegerSet messageIds = this.clientidIndex.get(inboundMessageStatus.clientId());
			if (messageIds == null) {
				messageIds = new SerializableIntegerSet();
			}
			messageIds.add(inboundMessageStatus.messageId());
			this.clientidIndex.put(inboundMessageStatus.clientId(), messageIds);

			MessageReferenceCounts.INSTANCE.retain(inboundMessageStatus.messageKey());

			logger.debug("InboundMessageStatus removed [messageId={}, clientId={}, status=]",
					inboundMessageStatus.messageId(), inboundMessageStatus.clientId(), inboundMessageStatus.status());
			logger.debug("InboundMessageStatuses Size [data={}, messageidIndex={}, clientidIndex={}]", data.size(),
					messageidIndex.size(), clientidIndex.size());
		}
		finally {
			modifyLock.unlock();
		}
	}

	public InboundMessageStatus getBy(Integer messageId, String clientId) {
		return data.get(key(messageId, clientId));
	}

	public InboundMessageStatus getByKey(String key) {
		return data.get(key);
	}

	public InboundMessageStatus removeByKey(Integer messageId, String clientId) {
		return removeByKey(key(messageId, clientId));
	}

	private InboundMessageStatus removeByKey(String key) {
		modifyLock.lock();

		try {
			InboundMessageStatus removed = this.data.remove(key);
			if (removed == null) { return null; }

			SerializableStringSet clientIds = messageidIndex.get(removed.messageId());
			clientIds.remove(removed.clientId());
			if (clientIds.size() <= 0) {
				messageidIndex.remove(removed.messageId());
			}
			else {
				messageidIndex.put(removed.messageId(), clientIds);
			}

			SerializableIntegerSet messageIds = clientidIndex.get(removed.clientId());
			messageIds.remove(removed.messageId());
			if (messageIds.size() <= 0) {
				clientidIndex.remove(removed.clientId());
			}
			else {
				clientidIndex.put(removed.clientId(), messageIds);
			}

			MessageReferenceCounts.INSTANCE.release(removed.messageKey());

			logger.debug("InboundMessageStatus removed [messageId={}, clientId={}, status=]", removed.messageId(),
					removed.clientId(), removed.status());
			logger.debug("InboundMessageStatuses Size [data={}, messageidIndex={}, clientidIndex={}]", data.size(),
					messageidIndex.size(), clientidIndex.size());
			return removed;
		}
		finally {
			modifyLock.unlock();
		}
	}

	public void update(Integer messageId, String clientId, InboundMessageStatus.Status targetStatus) {
		String key = key(messageId, clientId);

		InboundMessageStatus status = data.get(key);
		if (status == null) { return; }

		status.status(targetStatus);

		data.put(key, status);

		logger.debug("InboundMessageStatus removed [messageId={}, clientId={}, status=]", status.messageId(),
				status.clientId(), status.status());
	}
}

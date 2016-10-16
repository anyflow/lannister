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

public class OutboundMessageStatuses {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OutboundMessageStatuses.class);

	private final Map<String, OutboundMessageStatus> data;
	private final Map<Integer, SerializableStringSet> messageidIndex;
	private final Map<String, SerializableIntegerSet> clientidIndex;

	private final Lock modifyLock;

	protected OutboundMessageStatuses() {
		this.data = ClusterDataFactory.INSTANCE.createMap("OutboundMessageStatuses_data");
		this.messageidIndex = ClusterDataFactory.INSTANCE.createMap("OutboundMessageStatuses_messageidIndex");
		this.clientidIndex = ClusterDataFactory.INSTANCE.createMap("OutboundMessageStatuses_clientidIndex");

		this.modifyLock = ClusterDataFactory.INSTANCE.createLock("OutboundMessageStatuses_modifyLock");
	}

	public static String key(Integer messageId, String clientId) {
		return clientId + "_" + Integer.toString(messageId);
	}

	public void put(OutboundMessageStatus outboundMessageStatus) {
		if (outboundMessageStatus == null) { return; }

		modifyLock.lock();
		try {
			this.data.put(outboundMessageStatus.key(), outboundMessageStatus);

			SerializableStringSet clientIds = this.messageidIndex.get(outboundMessageStatus.messageId());
			if (clientIds == null) {
				clientIds = new SerializableStringSet();
			}
			clientIds.add(outboundMessageStatus.clientId());
			this.messageidIndex.put(outboundMessageStatus.messageId(), clientIds);

			SerializableIntegerSet messageIds = this.clientidIndex.get(outboundMessageStatus.clientId());
			if (messageIds == null) {
				messageIds = new SerializableIntegerSet();
			}
			messageIds.add(outboundMessageStatus.messageId());
			this.clientidIndex.put(outboundMessageStatus.clientId(), messageIds);

			MessageReferenceCounts.INSTANCE.retain(outboundMessageStatus.messageKey());

			logger.debug("OutboundMessageStatus added [messageId={}, clientId={}, status=]",
					outboundMessageStatus.messageId(), outboundMessageStatus.clientId(),
					outboundMessageStatus.status());
			logger.debug("OutboundMessageStatuses Size [data={}, messageidIndex={}, clientidIndex={}]", data.size(),
					messageidIndex.size(), clientidIndex.size());
		}
		finally {
			modifyLock.unlock();
		}
	}

	public OutboundMessageStatus getBy(Integer messageId, String clientId) {
		return data.get(key(messageId, clientId));
	}

	public Set<Integer> messageIdsOf(String clientId) {
		Set<Integer> ret = clientidIndex.get(clientId);

		return ret == null ? Sets.newHashSet() : ret;
	}

	public OutboundMessageStatus removeByKey(Integer messageId, String clientId) {
		return removeByKey(key(messageId, clientId));
	}

	private OutboundMessageStatus removeByKey(String key) {
		modifyLock.lock();

		try {
			OutboundMessageStatus removed = this.data.remove(key);
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

			logger.debug("OutboundMessageStatus removed [messageId={}, clientId={}, status=]", removed.messageId(),
					removed.clientId(), removed.status());
			logger.debug("OutboundMessageStatuses Size [data={}, messageidIndex={}, clientidIndex={}]", data.size(),
					messageidIndex.size(), clientidIndex.size());

			return removed;
		}
		finally {
			modifyLock.unlock();
		}
	}

	public Set<Integer> removeByClientId(String clientId) {
		modifyLock.lock();

		try {
			SerializableIntegerSet messageIds = this.clientidIndex.remove(clientId);
			if (messageIds == null) { return Sets.newHashSet(); }

			messageIds.forEach(messageId -> {
				SerializableStringSet clientIds = messageidIndex.get(messageId);
				clientIds.remove(clientId);
				if (clientIds.size() <= 0) {
					messageidIndex.remove(messageId);
				}
				else {
					messageidIndex.put(messageId, clientIds);
				}
			});
			messageIds.stream().map(messageId -> key(messageId, clientId)).forEach(key -> {
				OutboundMessageStatus removed = data.remove(key);

				MessageReferenceCounts.INSTANCE.release(removed.messageKey());

				logger.debug("OutboundMessageStatus removed [messageId={}, clientId={}, status=]", removed.messageId(),
						removed.clientId(), removed.status());
				logger.debug("OutboundMessageStatuses Size [data={}, messageidIndex={}, clientidIndex={}]", data.size(),
						messageidIndex.size(), clientidIndex.size());
			});

			return messageIds;
		}
		finally {
			modifyLock.unlock();
		}
	}

	public boolean containsKey(Integer messageId, String clientId) {
		return data.containsKey(key(messageId, clientId));
	}

	public void update(Integer messageId, String clientId, OutboundMessageStatus.Status targetStatus) {
		String key = key(messageId, clientId);

		OutboundMessageStatus status = data.get(key);
		if (status == null) { return; }

		status.status(targetStatus);

		data.put(key, status);

		logger.debug("OutboundMessageStatus updated [messageId={}, clientId={}, status=]", status.messageId(),
				status.clientId(), status.status());
	}
}

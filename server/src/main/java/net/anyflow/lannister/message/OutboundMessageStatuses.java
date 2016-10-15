package net.anyflow.lannister.message;

import java.util.Set;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.Sets;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;
import net.anyflow.lannister.cluster.SerializableIntegerSet;
import net.anyflow.lannister.cluster.SerializableStringSet;

public class OutboundMessageStatuses {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OutboundMessageStatuses.class);

	private final Map<String, OutboundMessageStatus> data;
	private final Map<Integer, SerializableStringSet> messageidIndex;
	private final Map<String, SerializableIntegerSet> clientidIndex;

	private final Lock putLock;
	private final Lock removeLock;

	protected OutboundMessageStatuses() {
		this.data = ClusterDataFactory.INSTANCE.createMap("OutboundMessageStatuses_data");
		this.messageidIndex = ClusterDataFactory.INSTANCE.createMap("OutboundMessageStatuses_messageidIndex");
		this.clientidIndex = ClusterDataFactory.INSTANCE.createMap("OutboundMessageStatuses_clientidIndex");

		this.putLock = ClusterDataFactory.INSTANCE.createLock("OutboundMessageStatuses_putLock");
		this.removeLock = ClusterDataFactory.INSTANCE.createLock("OutboundMessageStatuses_removeLock");
	}

	public static String key(Integer messageId, String clientId) {
		return clientId + "_" + Integer.toString(messageId);
	}

	public void put(OutboundMessageStatus outboundMessageStatus) {
		if (outboundMessageStatus == null) { return; }

		putLock.lock();
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
		}
		finally {
			putLock.unlock();
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
		removeLock.lock();

		try {
			OutboundMessageStatus removed = this.data.remove(key);
			if (removed == null) { return null; }

			this.messageidIndex.remove(removed.messageId());
			this.clientidIndex.remove(removed.clientId());

			MessageReferenceCounts.INSTANCE.release(removed.messageKey());

			return removed;
		}
		finally {
			removeLock.unlock();
		}
	}

	public Set<Integer> removeByClientId(String clientId) {
		removeLock.lock();

		try {
			SerializableIntegerSet messageIds = this.clientidIndex.remove(clientId);
			if (messageIds == null) { return Sets.newHashSet(); }

			messageIds.forEach(messageId -> this.messageidIndex.get(messageId).remove(clientId));
			messageIds.stream().map(messageId -> key(messageId, clientId)).forEach(key -> {
				OutboundMessageStatus removed = data.remove(key);
				MessageReferenceCounts.INSTANCE.release(removed.messageKey());
			});

			return messageIds;
		}
		finally {
			removeLock.unlock();
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
	}
}

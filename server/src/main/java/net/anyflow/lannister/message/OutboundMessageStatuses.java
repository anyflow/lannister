package net.anyflow.lannister.message;

import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.Lists;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;
import net.anyflow.lannister.cluster.SerializableIntegerList;
import net.anyflow.lannister.cluster.SerializableStringList;

public class OutboundMessageStatuses {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OutboundMessageStatuses.class);

	private final Map<String, OutboundMessageStatus> data;
	private final Map<Integer, SerializableStringList> messageidIndex;
	private final Map<String, SerializableIntegerList> clientidIndex;

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

	public int size() {
		return data.size();
	}

	public void put(OutboundMessageStatus outboundMessageStatus) {
		if (outboundMessageStatus == null) { return; }

		putLock.lock();
		try {
			this.data.put(outboundMessageStatus.key(), outboundMessageStatus);

			SerializableStringList clientIds = this.messageidIndex.get(outboundMessageStatus.messageId());
			if (clientIds == null) {
				clientIds = new SerializableStringList();
				this.messageidIndex.put(outboundMessageStatus.messageId(), clientIds);
			}
			clientIds.add(outboundMessageStatus.clientId());

			SerializableIntegerList messageIds = this.clientidIndex.get(outboundMessageStatus.clientId());
			if (messageIds == null) {
				messageIds = new SerializableIntegerList();
				this.clientidIndex.put(outboundMessageStatus.clientId(), messageIds);
			}
			messageIds.add(outboundMessageStatus.messageId());

			MessageReferenceCounts.INSTANCE.retain(outboundMessageStatus.messageKey());
		}
		finally {
			putLock.unlock();
		}
	}

	public Set<Integer> messageIds() {
		return messageidIndex.keySet();
	}

	public OutboundMessageStatus getBy(Integer messageId, String clientId) {
		return data.get(key(messageId, clientId));
	}

	public List<String> getClientIdsOf(Integer messageId) {
		List<String> ret = messageidIndex.get(messageId);

		return ret == null ? Lists.newArrayList() : ret;
	}

	public List<Integer> getMessageIdsOf(String clientId) {
		List<Integer> ret = clientidIndex.get(clientId);

		return ret == null ? Lists.newArrayList() : ret;
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

	public List<Integer> removeByClientId(String clientId) {
		removeLock.lock();

		try {
			SerializableIntegerList messageIds = this.clientidIndex.remove(clientId);
			if (messageIds == null) { return Lists.newArrayList(); }

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

	public boolean containsClientId(String clientId) {
		return this.clientidIndex.containsKey(clientId);
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

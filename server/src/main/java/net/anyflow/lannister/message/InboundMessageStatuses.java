package net.anyflow.lannister.message;

import java.util.Set;
import java.util.concurrent.locks.Lock;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;
import net.anyflow.lannister.cluster.SerializableIntegerSet;
import net.anyflow.lannister.cluster.SerializableStringSet;

public class InboundMessageStatuses {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InboundMessageStatuses.class);

	private final Map<String, InboundMessageStatus> data;
	private final Map<Integer, SerializableStringSet> messageidIndex;
	private final Map<String, SerializableIntegerSet> clientidIndex;

	private final Lock putLock;
	private final Lock removeLock;

	protected InboundMessageStatuses() {
		this.data = ClusterDataFactory.INSTANCE.createMap("InboundMessageStatuses_data");
		this.messageidIndex = ClusterDataFactory.INSTANCE.createMap("InboundMessageStatuses_messageidIndex");
		this.clientidIndex = ClusterDataFactory.INSTANCE.createMap("InboundMessageStatuses_clientidIndex");

		this.putLock = ClusterDataFactory.INSTANCE.createLock("InboundMessageStatuses_putLock");
		this.removeLock = ClusterDataFactory.INSTANCE.createLock("InboundMessageStatuses_removeLock");
	}

	public static String key(Integer messageId, String clientId) {
		return clientId + "_" + Integer.toString(messageId);
	}

	public Set<String> keySet() {
		return data.keySet();
	}

	public void put(InboundMessageStatus inboundMessageStatus) {
		if (inboundMessageStatus == null) { return; }

		putLock.lock();
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
		}
		finally {
			putLock.unlock();
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
		removeLock.lock();

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

			return removed;
		}
		finally {
			removeLock.unlock();
		}
	}

	public void update(Integer messageId, String clientId, InboundMessageStatus.Status targetStatus) {
		String key = key(messageId, clientId);

		InboundMessageStatus status = data.get(key);
		if (status == null) { return; }

		status.status(targetStatus);

		data.put(key, status);
	}
}

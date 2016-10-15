package net.anyflow.lannister.message;

import java.util.concurrent.locks.Lock;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;

public class MessageReferenceCounts {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageReferenceCounts.class);

	public static final MessageReferenceCounts INSTANCE = new MessageReferenceCounts();

	private final Map<String, Integer> data; // key : message.key()

	@JsonIgnore
	private Lock lock;

	private MessageReferenceCounts() {
		this.data = ClusterDataFactory.INSTANCE.createMap("MessagesReferenceCounts_data");
		this.lock = ClusterDataFactory.INSTANCE.createLock("MessagesReferenceCounts_lock");

	}

	public void retain(String messageKey) {
		lock.lock();

		try {
			Integer count = data.get(messageKey);
			if (count == null) {
				count = 0;
			}

			data.put(messageKey, ++count);
			logger.debug("message reference added [count={}, messageKey={}]", count, messageKey);
		}
		finally {
			lock.unlock();
		}
	}

	public void release(String messageKey) {
		Integer count = data.get(messageKey);
		if (count == null) { return; }

		lock.lock();

		try {
			if (count <= 0) {
				logger.error("Message reference count error [key={}, count={}]", messageKey, count);
				return;
			}
			else if (count == 1) {
				data.remove(messageKey);
				Message.NEXUS.remove(messageKey);
				logger.debug("message removed [messageKey={}]", messageKey);
			}
			else {
				data.put(messageKey, --count);
				logger.debug("message reference released [count={}, messageKey={}]", count, messageKey);
			}
		}
		finally {
			lock.unlock();
		}
	}
}

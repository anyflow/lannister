package net.anyflow.lannister.message;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;

public class Messages {
	@SuppressWarnings("unused")
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
		return data.remove(key);
	}

	public Message get(String key) {
		return data.get(key);
	}

	public void put(String key, Message message) {
		data.put(key, message);
	}
}

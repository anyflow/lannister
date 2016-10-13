package net.anyflow.lannister.topic;

import java.util.List;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.Lists;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;
import net.anyflow.lannister.cluster.SerializableStringList;

public class TopicSubscribers {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TopicSubscribers.class);

	private final Map<String, TopicSubscriber> data;
	private final Map<String, SerializableStringList> topicnameIndex;
	private final Map<String, SerializableStringList> clientidIndex;

	protected TopicSubscribers() {
		this.data = ClusterDataFactory.INSTANCE.createMap("TopicSubscribers");
		this.topicnameIndex = ClusterDataFactory.INSTANCE.createMap("TopicSubscribers_topicnameIndex");
		this.clientidIndex = ClusterDataFactory.INSTANCE.createMap("TopicSubscribers_clientidIndex");
	}

	public static String key(String topicName, String clientId) {
		return topicName + "_" + clientId;
	}

	public void put(TopicSubscriber topicSubscriber) {
		if (topicSubscriber == null) { return; }

		Lock lock = ClusterDataFactory.INSTANCE.createLock(topicSubscriber.key());

		lock.lock();
		try {
			this.data.put(topicSubscriber.key(), topicSubscriber);

			SerializableStringList keys = this.topicnameIndex.get(topicSubscriber.topicName());
			if (keys == null) {
				keys = new SerializableStringList();
				this.topicnameIndex.put(topicSubscriber.topicName(), keys);
			}
			keys.add(topicSubscriber.key());

			keys = this.clientidIndex.get(topicSubscriber.clientId());
			if (keys == null) {
				keys = new SerializableStringList();
				this.clientidIndex.put(topicSubscriber.clientId(), keys);
			}
			keys.add(topicSubscriber.key());
		}
		finally {
			lock.unlock();
		}
	}

	public TopicSubscriber getBy(String topicName, String clientId) {
		return data.get(key(topicName, clientId));
	}

	private List<TopicSubscriber> getFrom(String filter, Map<String, SerializableStringList> index) {
		List<TopicSubscriber> ret = Lists.newArrayList();

		List<String> keys = index.get(filter);
		if (keys == null || keys.size() <= 0) { return ret; }

		keys.forEach(key -> ret.add(data.get(key)));

		return ret;
	}

	public List<TopicSubscriber> getByTopicName(String topicName) {
		return getFrom(topicName, topicnameIndex);
	}

	public List<TopicSubscriber> getByClientId(String clientId) {
		return getFrom(clientId, clientidIndex);
	}

	public void removeByKey(String key) {
		Lock lock = ClusterDataFactory.INSTANCE.createLock(key);

		lock.lock();
		try {
			TopicSubscriber removed = this.data.remove(key);
			if (removed == null) { return; }

			this.topicnameIndex.remove(removed.topicName());
			this.clientidIndex.remove(removed.clientId());
		}
		finally {
			lock.unlock();
		}
	}

	public void removeByKey(String topicName, String clientId) {
		removeByKey(key(topicName, clientId));
	}
}
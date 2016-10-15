package net.anyflow.lannister.topic;

import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.Lists;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;
import net.anyflow.lannister.cluster.SerializableStringList;

public class TopicSubscriptions {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TopicSubscriptions.class);

	private final Map<String, TopicSubscription> data;
	private final Map<String, SerializableStringList> topicfilterIndex;
	private final Map<String, SerializableStringList> clientidIndex;

	private final Lock putLock;
	private final Lock removeLock;

	protected TopicSubscriptions() {
		this.data = ClusterDataFactory.INSTANCE.createMap("TopicSubscriptions_data");
		this.topicfilterIndex = ClusterDataFactory.INSTANCE.createMap("TopicSubscriptions_topicfilterIndex");
		this.clientidIndex = ClusterDataFactory.INSTANCE.createMap("TopicSubscriptions_clientidIndex");

		this.putLock = ClusterDataFactory.INSTANCE.createLock("TopicSubscriptions_putLock");
		this.removeLock = ClusterDataFactory.INSTANCE.createLock("TopicSubscriptions_removeLock");
	}

	public static String key(String topicFilter, String clientId) {
		return topicFilter + "_" + clientId;
	}

	public int size() {
		return data.size();
	}

	public void put(TopicSubscription topicSubscription) {
		if (topicSubscription == null) { return; }

		putLock.lock();
		try {
			this.data.put(topicSubscription.key(), topicSubscription);

			SerializableStringList clientIds = this.topicfilterIndex.get(topicSubscription.topicFilter());
			if (clientIds == null) {
				clientIds = new SerializableStringList();
				this.topicfilterIndex.put(topicSubscription.topicFilter(), clientIds);
			}
			clientIds.add(topicSubscription.clientId());

			SerializableStringList topicNames = this.clientidIndex.get(topicSubscription.clientId());
			if (topicNames == null) {
				topicNames = new SerializableStringList();
				this.clientidIndex.put(topicSubscription.clientId(), topicNames);
			}
			topicNames.add(topicSubscription.topicFilter());

			Topic.NEXUS.keySet().stream()
					.filter(topicName -> TopicMatcher.match(topicSubscription.topicFilter(), topicName))
					.forEach(topicName -> TopicSubscriber.NEXUS
							.put(new TopicSubscriber(topicSubscription.clientId(), topicName)));
		}
		finally {
			putLock.unlock();
		}
	}

	public Set<String> topicFilters() {
		return topicfilterIndex.keySet();
	}

	public TopicSubscription getBy(String topicFilter, String clientId) {
		return data.get(key(topicFilter, clientId));
	}

	public List<String> getClientIdsOf(String topicFilter) {
		List<String> ret = topicfilterIndex.get(topicFilter);

		return ret == null ? Lists.newArrayList() : ret;
	}

	public List<String> getTopicFiltersOf(String clientId) {
		List<String> ret = clientidIndex.get(clientId);

		return ret == null ? Lists.newArrayList() : ret;
	}

	public TopicSubscription removeByKey(String topicFilter, String clientId) {
		return removeByKey(key(topicFilter, clientId));
	}

	private TopicSubscription removeByKey(String key) {
		removeLock.lock();

		try {
			TopicSubscription removed = this.data.remove(key);
			if (removed == null) { return null; }

			this.topicfilterIndex.remove(removed.topicFilter());
			this.clientidIndex.remove(removed.clientId());

			return removed;
		}
		finally {
			removeLock.unlock();
		}
	}

	public List<String> removeByClientId(String clientId) {
		removeLock.lock();

		try {
			SerializableStringList topicFilters = this.clientidIndex.remove(clientId);
			if (topicFilters == null) { return Lists.newArrayList(); }

			topicFilters.forEach(topicFilter -> this.topicfilterIndex.get(topicFilter).remove(clientId));
			topicFilters.stream().map(topicFilter -> key(topicFilter, clientId)).forEach(key -> data.remove(key));

			return topicFilters;
		}
		finally {
			removeLock.unlock();
		}
	}

	public boolean containsClientId(String clientId) {
		return this.clientidIndex.containsKey(clientId);
	}
}

package net.anyflow.lannister.topic;

import java.util.Set;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.Sets;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;
import net.anyflow.lannister.cluster.SerializableStringSet;

public class TopicSubscriptions {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TopicSubscriptions.class);

	private final Map<String, TopicSubscription> data;
	private final Map<String, SerializableStringSet> topicfilterIndex;
	private final Map<String, SerializableStringSet> clientidIndex;

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

			SerializableStringSet clientIds = this.topicfilterIndex.get(topicSubscription.topicFilter());
			if (clientIds == null) {
				clientIds = new SerializableStringSet();
			}
			clientIds.add(topicSubscription.clientId());
			this.topicfilterIndex.put(topicSubscription.topicFilter(), clientIds);

			SerializableStringSet topicFilters = this.clientidIndex.get(topicSubscription.clientId());
			if (topicFilters == null) {
				topicFilters = new SerializableStringSet();
			}
			topicFilters.add(topicSubscription.topicFilter());
			this.clientidIndex.put(topicSubscription.clientId(), topicFilters);

			Topic.NEXUS.keySet().stream()
					.filter(topicName -> TopicMatcher.match(topicSubscription.topicFilter(), topicName))
					.forEach(topicName -> TopicSubscriber.NEXUS
							.put(new TopicSubscriber(topicSubscription.clientId(), topicName)));

			logger.debug("TopicSubscription added [topicFilter={}, clientId={}]", topicSubscription.topicFilter(),
					topicSubscription.clientId());
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

	public Set<String> clientIdsOf(String topicFilter) {
		Set<String> ret = topicfilterIndex.get(topicFilter);

		return ret == null ? Sets.newHashSet() : ret;
	}

	public Set<String> topicFiltersOf(String clientId) {
		Set<String> ret = clientidIndex.get(clientId);

		return ret == null ? Sets.newHashSet() : ret;
	}

	public TopicSubscription removeByKey(String topicFilter, String clientId) {
		return removeByKey(key(topicFilter, clientId));
	}

	private TopicSubscription removeByKey(String key) {
		removeLock.lock();

		try {
			TopicSubscription removed = this.data.remove(key);
			if (removed == null) { return null; }

			SerializableStringSet clientIds = topicfilterIndex.get(removed.topicFilter());
			clientIds.remove(removed.clientId());

			if (clientIds.size() <= 0) {
				topicfilterIndex.remove(removed.topicFilter());
			}
			else {
				topicfilterIndex.put(removed.topicFilter(), clientIds);
			}

			SerializableStringSet topicFilters = clientidIndex.get(removed.clientId());
			topicFilters.remove(removed.topicFilter());

			if (topicFilters.size() <= 0) {
				clientidIndex.remove(removed.clientId());
			}
			else {
				clientidIndex.put(removed.clientId(), topicFilters);
			}

			logger.debug("TopicSubscription removed [topicFilter={}, clientId={}]", removed.topicFilter(),
					removed.clientId());

			return removed;
		}
		finally {
			removeLock.unlock();
		}
	}

	public Set<String> removeByClientId(String clientId) {
		removeLock.lock();

		try {
			SerializableStringSet topicFilters = this.clientidIndex.remove(clientId);
			if (topicFilters == null) { return Sets.newHashSet(); }

			topicFilters.forEach(topicFilter -> {
				SerializableStringSet target = topicfilterIndex.get(topicFilter);
				target.remove(clientId);

				if (target.size() <= 0) {
					topicfilterIndex.remove(topicFilter);
				}
				else {
					topicfilterIndex.put(topicFilter, target);
				}
			});

			topicFilters.stream().map(topicFilter -> key(topicFilter, clientId)).forEach(key -> {
				TopicSubscription removed = data.remove(key);

				logger.debug("TopicSubscription removed [topicFilter={}, clientId={}]", removed.topicFilter(),
						removed.clientId());
			});

			return topicFilters;
		}
		finally {
			removeLock.unlock();
		}
	}
}

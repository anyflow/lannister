package net.anyflow.lannister.topic;

import java.util.Set;
import java.util.concurrent.locks.Lock;

import com.google.common.collect.Sets;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;
import net.anyflow.lannister.cluster.SerializableStringSet;

public class TopicSubscribers {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TopicSubscribers.class);

	private final Map<String, TopicSubscriber> data;
	private final Map<String, SerializableStringSet> topicnameIndex;
	private final Map<String, SerializableStringSet> clientidIndex;

	private final Lock putLock;
	private final Lock removeLock;

	protected TopicSubscribers() {
		this.data = ClusterDataFactory.INSTANCE.createMap("TopicSubscribers_data");
		this.topicnameIndex = ClusterDataFactory.INSTANCE.createMap("TopicSubscribers_topicnameIndex");
		this.clientidIndex = ClusterDataFactory.INSTANCE.createMap("TopicSubscribers_clientidIndex");

		this.putLock = ClusterDataFactory.INSTANCE.createLock("TopicSubscribers_putLock");
		this.removeLock = ClusterDataFactory.INSTANCE.createLock("TopicSubscribers_removeLock");
	}

	public static String key(String topicName, String clientId) {
		return topicName + "_" + clientId;
	}

	public void put(TopicSubscriber topicSubscriber) {
		if (topicSubscriber == null) { return; }

		putLock.lock();
		try {
			this.data.put(topicSubscriber.key(), topicSubscriber);

			SerializableStringSet clientIds = this.topicnameIndex.get(topicSubscriber.topicName());
			if (clientIds == null) {
				clientIds = new SerializableStringSet();
			}
			clientIds.add(topicSubscriber.clientId());
			this.topicnameIndex.put(topicSubscriber.topicName(), clientIds);

			SerializableStringSet topicNames = this.clientidIndex.get(topicSubscriber.clientId());
			if (topicNames == null) {
				topicNames = new SerializableStringSet();
			}
			topicNames.add(topicSubscriber.topicName());
			this.clientidIndex.put(topicSubscriber.clientId(), topicNames);

			logger.debug("TopicSubscriber added [topicName={}, clientId={}]", topicSubscriber.topicName(),
					topicSubscriber.clientId());
		}
		finally {
			putLock.unlock();
		}
	}

	public Set<String> clientIdsOf(String topicName) {
		Set<String> ret = topicnameIndex.get(topicName);

		return ret == null ? Sets.newHashSet() : ret;
	}

	public Set<String> topicNamesOf(String clientId) {
		Set<String> ret = clientidIndex.get(clientId);

		return ret == null ? Sets.newHashSet() : ret;
	}

	public void updateByTopicName(String topicName) {
		TopicSubscription.NEXUS.topicFilters().stream()
				.filter(topicFilter -> TopicMatcher.match(topicFilter, topicName))
				.forEach(topicFilter -> TopicSubscription.NEXUS.clientIdsOf(topicFilter)
						.forEach(clientId -> TopicSubscriber.NEXUS.put(new TopicSubscriber(clientId, topicName))));
	}

	public TopicSubscriber removeByKey(String topicName, String clientId) {
		return removeByKey(key(topicName, clientId));
	}

	private TopicSubscriber removeByKey(String key) {
		removeLock.lock();

		try {
			TopicSubscriber removed = this.data.remove(key);
			if (removed == null) { return null; }

			SerializableStringSet clientIds = topicnameIndex.get(removed.topicName());
			clientIds.remove(removed.clientId());
			if (clientIds.size() <= 0) {
				topicnameIndex.remove(removed.topicName());
			}
			else {
				topicnameIndex.put(removed.topicName(), clientIds);
			}

			SerializableStringSet topicNames = clientidIndex.get(removed.clientId());
			topicNames.remove(removed.topicName());
			if (topicNames.size() <= 0) {
				clientidIndex.remove(removed.clientId());
			}
			else {
				clientidIndex.put(removed.clientId(), topicNames);
			}

			logger.debug("TopicSubscriber removed [topicName={}, clientId={}]", removed.topicName(),
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
			SerializableStringSet topicNames = this.clientidIndex.remove(clientId);
			if (topicNames == null) { return Sets.newHashSet(); }

			topicNames.forEach(topicName -> {
				SerializableStringSet clientIds = topicnameIndex.get(topicName);
				clientIds.remove(clientId);
				if (clientIds.size() <= 0) {
					topicnameIndex.remove(topicName);
				}
				else {
					topicnameIndex.put(topicName, clientIds);
				}
			});

			topicNames.stream().map(topicName -> key(topicName, clientId)).forEach(key -> {
				TopicSubscriber removed = data.remove(key);

				logger.debug("TopicSubscriber removed [topicName={}, clientId={}]", removed.topicName(),
						removed.clientId());
			});

			return topicNames;
		}
		finally {
			removeLock.unlock();
		}
	}

	public void removeByTopicFilter(String clientId, String topicFilter) {
		// TODO optimization (use own logics of plural, not using removeByKey)
		this.topicNamesOf(clientId).stream().filter(topicName -> TopicMatcher.match(topicFilter, topicName))
				.forEach(topicName -> removeByKey(topicName, clientId));
	}
}
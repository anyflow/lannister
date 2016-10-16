package net.anyflow.lannister.cluster;

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.IdGenerator;

import net.anyflow.lannister.Settings;

public class ClusterDataFactory {
	private static final String ID = UUID.randomUUID().toString();

	public static final ClusterDataFactory INSTANCE = new ClusterDataFactory();

	private ClusterDataFactory() {
	}

	public String currentId() {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			return Hazelcast.INSTANCE.currentId();

		case IGNITE:
		case SINGLE:
			return ID;

		default:
			return null;
		}
	}

	public <K, V> Map<K, V> createMap(String name) {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			return new HazelcastMap<K, V>(name);

		case IGNITE:
			return new IgniteMap<K, V>(name);

		case SINGLE:
			return new NativeMap<K, V>(name);

		default:
			return null;
		}
	}

	public <K, V> Map<K, Set<V>> createSetValueMap(String name) {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			return new HazelcastSetValueMap<K, V>(name);

		case IGNITE:
		case SINGLE:
			return new NativeSetValueMap<K, V>(name);

		default:
			return null;
		}
	}

	public <V> Set<V> createSet(String name) {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			return new HazelcastSet<V>(name);

		case IGNITE:
		case SINGLE:
			return new NativeSet<V>();

		default:
			return null;
		}
	}

	public Lock createLock(String key) {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			return Hazelcast.INSTANCE.getLock(key);

		case IGNITE:
		case SINGLE:
			return new ReentrantLock();

		default:
			return null;
		}
	}

	public <E> ITopic<E> createTopic(String name) {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			return Hazelcast.INSTANCE.getTopic(name);

		case IGNITE:
		case SINGLE:
			return new SingleTopic<E>(name);

		default:
			return null;
		}
	}

	public IdGenerator createIdGenerator(String name) {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			return Hazelcast.INSTANCE.getIdGenerator(name);

		case IGNITE:
		case SINGLE:
			return new SingleIdGenerator(name);

		default:
			return null;
		}
	}
}
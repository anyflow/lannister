package net.anyflow.lannister.cluster;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.Maps;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.IdGenerator;

import net.anyflow.lannister.Settings;

public class Factory {
	private static final String ID = UUID.randomUUID().toString();

	public static final Factory INSTANCE = new Factory();

	private Factory() {
	}

	public String currentId() {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			return Hazelcast.INSTANCE.currentId();

		case IGNITE:
			return null;

		case SINGLE:
			return ID;

		default:
			return null;
		}

	}

	public <K, V> Map<K, V> createMap(String name) {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			return Hazelcast.INSTANCE.getMap(name);

		case IGNITE:
			return null;

		case SINGLE:
			return Maps.newConcurrentMap();

		default:
			return null;
		}
	}

	public Lock createLock(String key) {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			return Hazelcast.INSTANCE.getLock(key);

		case IGNITE:
			return null;

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
			return null;

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
			return null;

		case SINGLE:
			return new SingleIdGenerator(name);

		default:
			return null;
		}
	}
}
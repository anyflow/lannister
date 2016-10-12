package net.anyflow.lannister.cluster;

import java.util.Map;
import java.util.concurrent.locks.Lock;

import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import net.anyflow.lannister.Settings;

public class ClusterDataDisposer {
	public static final ClusterDataDisposer INSTANCE = new ClusterDataDisposer();

	private ClusterDataDisposer() {
	}

	public <K, V> void disposeMap(Map<K, V> map) {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			((IMap<K, V>) map).destroy();
			break;

		case IGNITE:
			break;

		case SINGLE:
			break;

		default:
			break;
		}
	}

	public void disposeLock(Lock lock) {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			((ILock) lock).destroy();
			break;

		case IGNITE:
			break;

		case SINGLE:
			break;

		default:
			break;
		}
	}

	public <E> void disposeTopic(ITopic<E> topic) {
		switch (Settings.INSTANCE.clusteringMode()) {
		case HAZELCAST:
			topic.destroy();
			break;

		case IGNITE:
			break;

		case SINGLE:
			topic.destroy();
			break;

		default:
			break;
		}
	}
}
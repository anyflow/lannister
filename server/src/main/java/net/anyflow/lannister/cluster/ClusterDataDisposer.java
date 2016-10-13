package net.anyflow.lannister.cluster;

import java.util.concurrent.locks.Lock;

import com.hazelcast.core.ILock;

import net.anyflow.lannister.Settings;

public class ClusterDataDisposer {
	public static final ClusterDataDisposer INSTANCE = new ClusterDataDisposer();

	private ClusterDataDisposer() {
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
}
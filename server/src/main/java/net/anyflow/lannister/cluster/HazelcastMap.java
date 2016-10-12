package net.anyflow.lannister.cluster;

import java.util.Collection;
import java.util.Set;

public class HazelcastMap<K, V> implements Map<K, V> {

	private com.hazelcast.core.IMap<K, V> engine;

	public HazelcastMap(String name) {
		engine = Hazelcast.INSTANCE.getMap(name);
	}

	@Override
	public void put(K key, V value) {
		engine.set(key, value);
	}

	@Override
	public V get(K key) {
		return engine.get(key);
	}

	@Override
	public V remove(K key) {
		return engine.remove(key);
	}

	@Override
	public Collection<V> values() {
		return engine.values();
	}

	@Override
	public int size() {
		return engine.size();
	}

	@Override
	public void dispose() {
		engine.destroy();
	}

	@Override
	public boolean containsKey(K key) {
		return engine.containsKey(key);
	}

	@Override
	public Set<K> keySet() {
		return engine.keySet();
	}
}
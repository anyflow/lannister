package net.anyflow.lannister.cluster;

import java.util.Set;

import com.google.common.collect.Maps;

public class NativeMap<K, V> implements Map<K, V> {

	private java.util.Map<K, V> engine;

	public NativeMap(String name) {
		engine = Maps.newConcurrentMap();
	}

	@Override
	public void put(K key, V value) {
		engine.put(key, value);
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
	public Set<K> keySet() {
		return engine.keySet();
	}

	@Override
	public int size() {
		return engine.size();
	}

	@Override
	public void dispose() {
		// DO NOTHING
	}

	@Override
	public boolean containsKey(K key) {
		return engine.containsKey(key);
	}
}
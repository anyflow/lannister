package net.anyflow.lannister.cluster;

import java.util.Collection;
import java.util.Set;

import org.apache.ignite.IgniteCache;

public class IgniteMap<K, V> implements Map<K, V> {

	private IgniteCache<K, V> engine;

	public IgniteMap(String name) {
		engine = Ignite.INSTANCE.getCache(name);
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
		return engine.getAndRemove(key);
	}

	@Override
	public Set<K> keySet() {
		throw new Error();
	}

	@Override
	public Collection<V> values() {
		throw new Error();
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
}
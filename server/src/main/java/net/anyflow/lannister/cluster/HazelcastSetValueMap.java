package net.anyflow.lannister.cluster;

import java.util.HashMap;

import com.google.common.collect.Maps;
import com.hazelcast.core.IdGenerator;

public class HazelcastSetValueMap<K, V> implements Map<K, Set<V>> {
	private static final IdGenerator ID_GENERATOR = Hazelcast.INSTANCE.getIdGenerator("HazelcastSetValueMap");

	private final com.hazelcast.core.IMap<K, String> engine;
	private final HashMap<String, Set<V>> values;
	private final String valueKeyPrefix;

	public HazelcastSetValueMap(String name) {
		engine = Hazelcast.INSTANCE.getMap(name);
		values = Maps.newHashMap();
		valueKeyPrefix = Long.toString(ID_GENERATOR.newId());
	}

	@Override
	public void put(K key, Set<V> value) {
		String valueKey = valueKey(key);

		Set<V> prev = values.put(valueKey, value);
		if (prev != null) {
			prev.dispose();
		}

		engine.set(key, valueKey);
	}

	private String valueKey(K key) {
		return valueKeyPrefix + "_" + key.toString();
	}

	@Override
	public Set<V> get(K key) {
		return values.get(engine.get(key));
	}

	@Override
	public Set<V> remove(K key) {
		String valueKey = engine.remove(key);
		if (valueKey == null) { return null; }

		return values.remove(valueKey);
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
	public java.util.Set<K> keySet() {
		return engine.keySet();
	}
}
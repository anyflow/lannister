package net.anyflow.lannister.cluster;

import java.util.Collection;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.anyflow.lannister.serialization.MapSerializer;

@JsonSerialize(using = MapSerializer.class)
public interface Map<K, V> {
	void put(K key, V value);

	V get(K key);

	V remove(K key);

	Set<K> keySet();

	Collection<V> values();

	void dispose();

	int size();

	boolean containsKey(K key);
}

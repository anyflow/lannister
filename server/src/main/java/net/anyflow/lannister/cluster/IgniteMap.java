/*
 * Copyright 2016 The Lannister Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.anyflow.lannister.cluster;

import java.util.Set;
import java.util.stream.Collectors;

import javax.cache.Cache;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.ScanQuery;

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
		return engine.query(new ScanQuery<K, V>()).getAll().stream().map(Cache.Entry::getKey)
				.collect(Collectors.toSet());
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
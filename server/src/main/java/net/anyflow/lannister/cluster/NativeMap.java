package net.anyflow.lannister.cluster;

import java.util.Set;

import com.google.common.collect.Maps;

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
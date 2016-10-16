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

import com.google.common.collect.Maps;

public class NativeSetValueMap<K, V> implements Map<K, Set<V>> {

	private final java.util.Map<K, Set<V>> engine;

	public NativeSetValueMap(String name) {
		engine = Maps.newHashMap();
	}

	@Override
	public void put(K key, Set<V> value) {
		Set<V> prev = engine.put(key, value);
		if (prev != null) {
			prev.dispose();
		}
	}

	@Override
	public Set<V> get(K key) {
		return engine.get(key);
	}

	@Override
	public Set<V> remove(K key) {
		return engine.remove(key);
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

	@Override
	public java.util.Set<K> keySet() {
		return engine.keySet();
	}
}
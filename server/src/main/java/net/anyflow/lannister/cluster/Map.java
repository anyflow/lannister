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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.anyflow.lannister.serialization.MapSerializer;

@JsonSerialize(using = MapSerializer.class)
public interface Map<K, V> {
	void put(K key, V value);

	V get(K key);

	V remove(K key);

	Set<K> keySet();

	void dispose();

	int size();

	boolean containsKey(K key);
}

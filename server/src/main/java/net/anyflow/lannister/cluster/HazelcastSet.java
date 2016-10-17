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

import java.util.stream.Stream;

import com.hazelcast.core.ISet;

public class HazelcastSet<V> implements Set<V> {
    private final ISet<V> engine;

    protected HazelcastSet(String name) {
        engine = Hazelcast.INSTANCE.getSet(name);
    }

    @Override
    public Stream<V> stream() {
        return engine.stream();
    }

    @Override
    public boolean remove(V value) {
        return engine.remove(value);
    }

    @Override
    public boolean add(V value) {
        return engine.add(value);
    }

    @Override
    public void dispose() {
        engine.destroy();
    }

    @Override
    public int size() {
        return engine.size();
    }
}
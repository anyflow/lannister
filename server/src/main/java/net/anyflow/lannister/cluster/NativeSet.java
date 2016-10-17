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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class NativeSet<V> implements Set<V> {
    private static java.util.Map<String, Object> SETS = Maps.newHashMap();

    private final java.util.Set<V> engine;
    private String name;
    
    public static <V> NativeSet<V> getOrCreate(String name) {
        @SuppressWarnings("unchecked")
        NativeSet<V> ret = (NativeSet<V>) SETS.get(name);
        
        if(ret == null) {
            ret = new NativeSet<V>(name);
            SETS.put(name, ret);
        }
        
        return ret;
    }
    
    private NativeSet(String name) {
        this.engine = Sets.newHashSet();
        this.name = name;
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
        SETS.remove(name);
    }

    @Override
    public int size() {
        return engine.size();
    }
}
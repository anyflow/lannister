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

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.IdGenerator;

import net.anyflow.lannister.Settings;

public class ClusterDataFactory {
    private static final String ID = UUID.randomUUID().toString();

    public static final ClusterDataFactory INSTANCE = new ClusterDataFactory();

    private ClusterDataFactory() {
    }

    public String currentId() {
        switch (Settings.INSTANCE.clusteringMode()) {
        case HAZELCAST:
            return Hazelcast.INSTANCE.currentId();

        case IGNITE:
        case SINGLE:
            return ID;

        default:
            return null;
        }
    }

    public <K, V> Map<K, V> createMap(String name) {
        switch (Settings.INSTANCE.clusteringMode()) {
        case HAZELCAST:
            return new HazelcastMap<K, V>(name);

        case IGNITE:
            return new IgniteMap<K, V>(name);

        case SINGLE:
            return new NativeMap<K, V>(name);

        default:
            return null;
        }
    }

    public <K, V> Map<K, Set<V>> createSetValueMap(String name) {
        switch (Settings.INSTANCE.clusteringMode()) {
        case HAZELCAST:
            return new HazelcastSetValueMap<K, V>(name);

        case IGNITE:
        case SINGLE:
            return new NativeSetValueMap<K, V>(name);

        default:
            return null;
        }
    }

    public <V> Set<V> getSet(String name) {
        switch (Settings.INSTANCE.clusteringMode()) {
        case HAZELCAST:
            return new HazelcastSet<V>(name);

        case IGNITE:
        case SINGLE:
            return NativeSet.getOrCreate(name);

        default:
            return null;
        }
    }

    public Lock createLock(String key) {
        switch (Settings.INSTANCE.clusteringMode()) {
        case HAZELCAST:
            return Hazelcast.INSTANCE.getLock(key);

        case IGNITE:
        case SINGLE:
            return new ReentrantLock();

        default:
            return null;
        }
    }

    public <E> ITopic<E> createTopic(String name) {
        switch (Settings.INSTANCE.clusteringMode()) {
        case HAZELCAST:
            return Hazelcast.INSTANCE.getTopic(name);

        case IGNITE:
        case SINGLE:
            return new SingleTopic<E>(name);

        default:
            return null;
        }
    }

    public IdGenerator createIdGenerator(String name) {
        switch (Settings.INSTANCE.clusteringMode()) {
        case HAZELCAST:
            return Hazelcast.INSTANCE.getIdGenerator(name);

        case IGNITE:
        case SINGLE:
            return new SingleIdGenerator(name);

        default:
            return null;
        }
    }
}
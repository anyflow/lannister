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
package net.anyflow.lannister.topic;

import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;
import net.anyflow.lannister.cluster.SerializableStringSet;

public class TopicSubscribers {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TopicSubscribers.class);

    private final Map<String, TopicSubscriber> data;
    private final Map<String, SerializableStringSet> topicnameIndex;
    private final Map<String, SerializableStringSet> clientidIndex;

    private final Lock modifyLock;

    protected TopicSubscribers() {
        this.data = ClusterDataFactory.INSTANCE.createMap("TopicSubscribers_data");
        this.topicnameIndex = ClusterDataFactory.INSTANCE.createMap("TopicSubscribers_topicnameIndex");
        this.clientidIndex = ClusterDataFactory.INSTANCE.createMap("TopicSubscribers_clientidIndex");

        this.modifyLock = ClusterDataFactory.INSTANCE.createLock("TopicSubscribers_modifyLock");
    }

    public static String key(String topicName, String clientId) {
        return topicName + "_" + clientId;
    }

    public void put(TopicSubscriber topicSubscriber) {
        if (topicSubscriber == null) { return; }

        modifyLock.lock();
        try {
            this.data.put(topicSubscriber.key(), topicSubscriber);

            SerializableStringSet clientIds = this.topicnameIndex.get(topicSubscriber.topicName());
            if (clientIds == null) {
                clientIds = new SerializableStringSet();
            }
            clientIds.add(topicSubscriber.clientId());
            this.topicnameIndex.put(topicSubscriber.topicName(), clientIds);

            SerializableStringSet topicNames = this.clientidIndex.get(topicSubscriber.clientId());
            if (topicNames == null) {
                topicNames = new SerializableStringSet();
            }
            topicNames.add(topicSubscriber.topicName());
            this.clientidIndex.put(topicSubscriber.clientId(), topicNames);

            logger.debug("TopicSubscriber added [topicName={}, clientId={}]", topicSubscriber.topicName(),
                    topicSubscriber.clientId());
            logger.debug("TopicSubscribers Size [data={}, topicnameIndex={}, clientidIndex={}]", data.size(),
                    topicnameIndex.size(), clientidIndex.size());
        }
        finally {
            modifyLock.unlock();
        }
    }

    public Set<String> clientIdsOf(String topicName) {
        Set<String> ret = topicnameIndex.get(topicName);

        return ret == null ? Sets.newHashSet() : ret;
    }

    public Set<String> topicNamesOf(String clientId) {
        Set<String> ret = clientidIndex.get(clientId);

        return ret == null ? Sets.newHashSet() : ret;
    }

    public void updateByTopicName(String topicName) {
        TopicSubscription.NEXUS.topicFilters().stream()
                .filter(topicFilter -> TopicMatcher.match(topicFilter, topicName))
                .forEach(topicFilter -> TopicSubscription.NEXUS.clientIdsOf(topicFilter).stream()
                        .forEach(clientId -> TopicSubscriber.NEXUS.put(new TopicSubscriber(clientId, topicName))));
    }

    public TopicSubscriber removeByKey(String topicName, String clientId) {
        return removeByKey(key(topicName, clientId));
    }

    private TopicSubscriber removeByKey(String key) {
        modifyLock.lock();
        try {
            TopicSubscriber removed = this.data.remove(key);
            if (removed == null) { return null; }

            SerializableStringSet clientIds = topicnameIndex.get(removed.topicName());
            clientIds.remove(removed.clientId());
            if (clientIds.size() <= 0) {
                topicnameIndex.remove(removed.topicName());
            }
            else {
                topicnameIndex.put(removed.topicName(), clientIds);
            }

            SerializableStringSet topicNames = clientidIndex.get(removed.clientId());
            topicNames.remove(removed.topicName());
            if (topicNames.size() <= 0) {
                clientidIndex.remove(removed.clientId());
            }
            else {
                clientidIndex.put(removed.clientId(), topicNames);
            }

            logger.debug("TopicSubscriber removed [topicName={}, clientId={}]", removed.topicName(),
                    removed.clientId());
            logger.debug("TopicSubscribers Size [data={}, topicnameIndex={}, clientidIndex={}]", data.size(),
                    topicnameIndex.size(), clientidIndex.size());

            return removed;
        }
        finally {
            modifyLock.unlock();
        }
    }

    public Set<String> removeByClientId(String clientId) {
        modifyLock.lock();
        try {
            SerializableStringSet topicNames = this.clientidIndex.remove(clientId);
            if (topicNames == null) { return Sets.newHashSet(); }

            topicNames.forEach(topicName -> {
                SerializableStringSet clientIds = topicnameIndex.get(topicName);
                clientIds.remove(clientId);
                if (clientIds.size() <= 0) {
                    topicnameIndex.remove(topicName);
                }
                else {
                    topicnameIndex.put(topicName, clientIds);
                }
            });

            topicNames.stream().map(topicName -> key(topicName, clientId)).forEach(key -> {
                TopicSubscriber removed = data.remove(key);

                logger.debug("TopicSubscriber removed [topicName={}, clientId={}]", removed.topicName(),
                        removed.clientId());
                logger.debug("TopicSubscribers Size [data={}, topicnameIndex={}, clientidIndex={}]", data.size(),
                        topicnameIndex.size(), clientidIndex.size());
            });

            return topicNames;
        }
        finally {
            modifyLock.unlock();
        }
    }

    public void removeByTopicFilter(String clientId, String topicFilter) {
        modifyLock.lock();
        try {
            List<String> topicNames = this.topicNamesOf(clientId).stream()
                    .filter(topicName -> TopicMatcher.match(topicFilter, topicName)).collect(Collectors.toList());

            topicNames.stream().map(topicName -> key(topicName, clientId)).forEach(key -> data.remove(key));

            SerializableStringSet topicValues = clientidIndex.get(clientId);
            if (topicValues != null) {
                topicValues.removeAll(topicNames);

                if (topicValues.size() <= 0) {
                    clientidIndex.remove(clientId);
                }
                else {
                    clientidIndex.put(clientId, topicValues);
                }
            }

            topicNames.forEach(topicName -> {
                SerializableStringSet clientIds = topicnameIndex.get(topicName);
                if (clientIds == null) { return; }

                clientIds.remove(clientId);
                if (clientIds.size() <= 0) {
                    topicnameIndex.remove(topicName);
                }
                else {
                    topicnameIndex.put(topicName, clientIds);
                }
            });

            logger.debug("TopicSubscribers removed [topicFilter={}, clientId={}, topicNameCount={}]", topicFilter,
                    clientId, topicNames.size());
            logger.debug("TopicSubscribers Size [data={}, topicnameIndex={}, clientidIndex={}]", data.size(),
                    topicnameIndex.size(), clientidIndex.size());
        }
        finally {
            modifyLock.unlock();
        }
    }
}
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

import java.util.concurrent.locks.Lock;

import net.anyflow.lannister.cluster.ClusterDataFactory;
import net.anyflow.lannister.cluster.Map;
import net.anyflow.lannister.cluster.Set;

public class TopicSubscriptions {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TopicSubscriptions.class);

    private final Map<String, TopicSubscription> data;
    private final Set<String> topicFilters;
    private final Set<String> clientIds;

    private final Lock modifyLock;

    protected TopicSubscriptions() {
        this.data = ClusterDataFactory.INSTANCE.createMap("TopicSubscriptions_data");
        this.topicFilters = ClusterDataFactory.INSTANCE.getSet("TopicSubscriptions_topicFilters");
        this.clientIds = ClusterDataFactory.INSTANCE.getSet("TopicSubscriptions_clientIds");
        this.modifyLock = ClusterDataFactory.INSTANCE.createLock("TopicSubscriptions_modifyLock");
    }

    public static String key(String topicFilter, String clientId) {
        return topicFilter + "_" + clientId;
    }

    public int size() {
        return data.size();
    }

    private Set<String> topicFilterIndexOf(String clientId) {
        return ClusterDataFactory.INSTANCE.getSet("TopicSubscriptions_topicfiltersOf_" + clientId);
    }

    private Set<String> clientIdIndexOf(String topicFilter) {
        return ClusterDataFactory.INSTANCE.getSet("TopicSubscriptions_clientIdsOf_" + topicFilter);
    }

    public void put(TopicSubscription topicSubscription) {
        if (topicSubscription == null) { return; }

        modifyLock.lock();
        try {
            this.data.put(topicSubscription.key(), topicSubscription);
            this.topicFilters.add(topicSubscription.topicFilter());
            this.clientIds.add(topicSubscription.clientId());

            Set<String> clientidIndex = clientIdIndexOf(topicSubscription.topicFilter());
            clientidIndex.add(topicSubscription.clientId());

            Set<String> topicfilterIndex = topicFilterIndexOf(topicSubscription.clientId());
            topicfilterIndex.add(topicSubscription.topicFilter());

            Topic.NEXUS.keySet().stream()
                    .filter(topicName -> TopicMatcher.match(topicSubscription.topicFilter(), topicName))
                    .forEach(topicName -> TopicSubscriber.NEXUS
                            .put(new TopicSubscriber(topicSubscription.clientId(), topicName)));

            logger.debug("TopicSubscription added [topicFilter={}, clientId={}, qos={}]",
                    topicSubscription.topicFilter(), topicSubscription.clientId(), topicSubscription.qos());
            logger.debug("TopicSubscriptions Size [data={}, topicfilterIndex={}, clientidIndex={}]", data.size(),
                    topicFilters.size(), clientIds.size());
        }
        finally {
            modifyLock.unlock();
        }
    }

    public Set<String> topicFilters() {
        return topicFilters;
    }

    public TopicSubscription getBy(String topicFilter, String clientId) {
        return data.get(key(topicFilter, clientId));
    }

    public Set<String> clientIdsOf(String topicFilter) {
        Set<String> ret = clientIdIndexOf(topicFilter);

        return ret == null ? ClusterDataFactory.INSTANCE.getSet("empty") : ret;
    }

    public Set<String> topicFiltersOf(String clientId) {
        Set<String> ret = topicFilterIndexOf(clientId);

        return ret == null ? ClusterDataFactory.INSTANCE.getSet("empty") : ret;
    }

    public TopicSubscription removeByKey(String topicFilter, String clientId) {
        return removeByKey(key(topicFilter, clientId));
    }

    private TopicSubscription removeByKey(String key) {
        modifyLock.lock();

        try {
            TopicSubscription removed = this.data.remove(key);
            if (removed == null) { return null; }

            Set<String> clientIdIndex = clientIdIndexOf(removed.topicFilter());
            clientIdIndex.remove(removed.clientId());

            if (clientIdIndex.size() <= 0) {
                clientIdIndex.dispose();
                topicFilters.remove(removed.topicFilter());
            }

            Set<String> topicFilterIndex = topicFilterIndexOf(removed.clientId());
            topicFilterIndex.remove(removed.topicFilter());

            if (topicFilterIndex.size() <= 0) {
                topicFilterIndex.dispose();
                clientIds.remove(removed.clientId());
            }

            logger.debug("TopicSubscription removed [topicFilter={}, clientId={}, qos={}]", removed.topicFilter(),
                    removed.clientId(), removed.qos());
            logger.debug("TopicSubscriptions Size [data={}, topicfilterIndex={}, clientidIndex={}]", data.size(),
                    topicFilters.size(), clientIds.size());

            return removed;
        }
        finally {
            modifyLock.unlock();
        }
    }

    public void removeByClientId(String clientId) {
        modifyLock.lock();

        try {
            if (!clientIds.remove(clientId)) { return; }

            Set<String> topicFilterIndex = topicFilterIndexOf(clientId);

            topicFilterIndex.stream().forEach(topicFilter -> {
                Set<String> clientIdIndex = clientIdIndexOf(topicFilter);
                clientIdIndex.remove(clientId);

                if (clientIdIndex.size() <= 0) {
                    clientIdIndex.dispose();
                    topicFilters.remove(topicFilter);
                }
            });

            topicFilterIndex.stream().map(topicFilter -> key(topicFilter, clientId)).forEach(key -> {
                TopicSubscription removed = data.remove(key);

                logger.debug("TopicSubscription removed [topicFilter={}, clientId={}, qos={}]", removed.topicFilter(),
                        removed.clientId(), removed.qos());
                logger.debug("TopicSubscriptions Size [data={}, topicfilterIndex={}, clientidIndex={}]", data.size(),
                        topicFilters.size(), clientIds.size());
            });
            
            topicFilterIndex.dispose();
        }
        finally {
            modifyLock.unlock();
        }
    }
}

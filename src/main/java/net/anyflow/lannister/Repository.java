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

package net.anyflow.lannister;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.hazelcast.config.Config;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import net.anyflow.lannister.message.InboundMessageStatus;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.serialization.JsonSerializer;
import net.anyflow.lannister.serialization.SerializableFactory;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Notification;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicSubscriber;
import net.anyflow.lannister.topic.TopicSubscription;

public class Repository {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Repository.class);

	public static Repository SELF;

	static {
		SELF = new Repository();
	}

	private final HazelcastInstance generator;

	private Repository() {
		Config config = new Config();

		config.getSerializationConfig().addPortableFactory(SerializableFactory.ID, new SerializableFactory());

		config.getSerializationConfig()
				.setClassDefinitions(Sets.newHashSet(Message.classDefinition(), InboundMessageStatus.classDefinition(),
						OutboundMessageStatus.classDefinition(), Session.classDefinition(),
						Notification.classDefinition(), Topic.classDefinition(), TopicSubscriber.classDefinition(),
						TopicSubscription.classDefinition()));

		config.getSerializationConfig().getSerializerConfigs().add(new SerializerConfig().setTypeClass(JsonNode.class)
				.setImplementation(JsonSerializer.makePlain(JsonNode.class)));

		generator = Hazelcast.newHazelcastInstance(config);
	}

	public HazelcastInstance generator() {
		return generator;
	}
}
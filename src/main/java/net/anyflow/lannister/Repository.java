package net.anyflow.lannister;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.hazelcast.config.Config;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.InboundMessageStatus;
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
						OutboundMessageStatus.classDefinition(), Session.classDefinition(), Notification.classDefinition(),
						Topic.classDefinition(), TopicSubscriber.classDefinition(),
						TopicSubscription.classDefinition()));

		config.getSerializationConfig().getSerializerConfigs().add(new SerializerConfig().setTypeClass(JsonNode.class)
				.setImplementation(JsonSerializer.makePlain(JsonNode.class)));

		generator = Hazelcast.newHazelcastInstance(config);
	}

	public HazelcastInstance generator() {
		return generator;
	}
}
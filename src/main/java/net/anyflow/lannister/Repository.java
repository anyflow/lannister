package net.anyflow.lannister;

import com.google.common.collect.Sets;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.ReceivedMessageStatus;
import net.anyflow.lannister.message.SentMessageStatus;
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
				.setClassDefinitions(Sets.newHashSet(Message.classDefinition(), ReceivedMessageStatus.classDefinition(),
						SentMessageStatus.classDefinition(), Session.classDefinition(), Notification.classDefinition(),
						Topic.classDefinition(), TopicSubscriber.classDefinition(),
						TopicSubscription.classDefinition()));

		generator = Hazelcast.newHazelcastInstance(config);
	}

	public HazelcastInstance generator() {
		return generator;
	}
}
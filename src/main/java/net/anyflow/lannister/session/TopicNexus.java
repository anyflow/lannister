package net.anyflow.lannister.session;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

public class TopicNexus {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TopicNexus.class);

	public static TopicNexus SELF;

	static {
		SELF = new TopicNexus();
	}

	private HazelcastInstance hcInstance;

	private TopicNexus() {
		Config config = new Config();

		hcInstance = Hazelcast.newHazelcastInstance(config);
	}

	public ITopic<String> get(String topicName) {
		return hcInstance.getTopic(topicName);
	}
}
package net.anyflow.lannister.session;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

public class Repository {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Repository.class);

	public static Repository SELF;

	static {
		SELF = new Repository();
	}

	private HazelcastInstance hcInstance;
	private IMap<String, Session> clientIdSessionMap;

	private Repository() {
		Config config = new Config();

		hcInstance = Hazelcast.newHazelcastInstance(config);
		clientIdSessionMap = hcInstance.getMap("clientIdSessionMap");
	}

	public ITopic<Message> topic(String topicFilter) {
		return hcInstance.getTopic(topicFilter);
	}

	public IMap<String, Session> clientIdSessionMap() {
		return clientIdSessionMap;
	}
}
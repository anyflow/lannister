package net.anyflow.lannister;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

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

		generator = Hazelcast.newHazelcastInstance(config);
	}

	public HazelcastInstance generator() {
		return generator;
	}
}
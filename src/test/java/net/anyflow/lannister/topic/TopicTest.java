package net.anyflow.lannister.topic;

import org.junit.BeforeClass;
import org.junit.Test;

import net.anyflow.lannister.MqttServerTest;
import net.anyflow.lannister.TestSuite;

public class TopicTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttServerTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestSuite.setUp();
	}

	@Test
	public void persistMessagesForDisconnectedPersistantSession() throws Exception {
		// TODO after mqttclient PUB/SUB featured
	}

}

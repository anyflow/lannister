package net.anyflow.lannister.session;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.TestSuite;
import net.anyflow.lannister.topic.TopicSubscription;

public class SessionTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestSuite.setUp();
	}

	@Test
	public void testMatches() throws Exception {
		String testTopic = "testTopic/test";
		Session session = new Session("1", "1", 1, 50, true, null);

		TopicSubscription ts0 = new TopicSubscription(session.clientId(), "testTopic/#", MqttQoS.AT_MOST_ONCE);
		TopicSubscription ts1 = new TopicSubscription(session.clientId(), "testTopic/+", MqttQoS.AT_LEAST_ONCE);
		TopicSubscription ts2 = new TopicSubscription(session.clientId(), testTopic, MqttQoS.EXACTLY_ONCE);

		TopicSubscription.NEXUS.put(ts0);
		TopicSubscription.NEXUS.put(ts1);
		TopicSubscription.NEXUS.put(ts2);

		Assert.assertEquals(3, TopicSubscription.NEXUS.topicFiltersOf(session.clientId()).size());

		TopicSubscription target = session.matches(testTopic);

		Assert.assertTrue(target.topicFilter().equals(testTopic));
	}
}

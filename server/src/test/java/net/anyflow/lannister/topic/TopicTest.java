package net.anyflow.lannister.topic;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.util.CharsetUtil;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.TestSuite;
import net.anyflow.lannister.TestUtil;
import net.anyflow.lannister.client.MqttClient;
import net.anyflow.lannister.message.ConnectOptions;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.Messages;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.server.MqttServerTest;
import net.anyflow.lannister.session.Session;

public class TopicTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttServerTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestSuite.setUp();
	}

	@Test
	public void persistMessagesForDisconnectedPersistantSession() throws Exception {
		String clientId = TestUtil.newClientId();

		ConnectOptions options = new ConnectOptions();
		options.clientId(clientId);
		options.cleanSession(false);

		String topicName = "testTopic";
		String message = "test message";

		MqttClient client = new MqttClient("mqtt://localhost:" + Settings.INSTANCE.mqttPort());
		client.connectOptions(options).connect();

		client.subscribe(new MqttTopicSubscription(topicName, MqttQoS.EXACTLY_ONCE));
		client.disconnect(true);

		Thread.sleep(100);

		Assert.assertNotNull(Session.NEXUS.get(clientId));

		String publisherId = TestUtil.newClientId();

		MqttClient publisher = new MqttClient("mqtt://localhost:" + Settings.INSTANCE.mqttPort());

		ConnectOptions pubOptions = new ConnectOptions();
		pubOptions.clientId(publisherId);
		pubOptions.cleanSession(true);

		int messageId = 1;
		publisher.connectOptions(pubOptions).connect();
		publisher.publish(new Message(messageId, topicName, publisherId, message.getBytes(CharsetUtil.UTF_8),
				MqttQoS.EXACTLY_ONCE, false));

		Thread.sleep(100);

		publisher.disconnect(true);

		Thread.sleep(1000);

		Assert.assertNull(Session.NEXUS.get(publisherId));

		Topic topic = Topic.NEXUS.get(topicName);
		Assert.assertNotNull(topic);

		final TopicSubscriber subscriber = TopicSubscriber.NEXUS.getBy(topic.name(), clientId);
		Assert.assertNotNull(subscriber);

		OutboundMessageStatus status = OutboundMessageStatus.NEXUS.getBy(messageId, clientId);

		Assert.assertNotNull(status);
		Assert.assertTrue(Messages.key(publisherId, messageId).equals(status.messageKey()));
	}
}
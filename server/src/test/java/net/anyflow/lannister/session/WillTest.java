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

package net.anyflow.lannister.session;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.util.CharsetUtil;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.TestSuite;
import net.anyflow.lannister.TestUtil;
import net.anyflow.lannister.client.MqttClient;
import net.anyflow.lannister.message.ConnectOptions;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.plugin.IMessage;
import net.anyflow.lannister.topic.Topic;

public class WillTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestSuite.setUp();
	}

	@Test
	public void testWillSend() throws Exception {
		String willTopic = "will";
		String message = "ASTALAVISTA";

		String client0Id = TestUtil.newClientId();
		ConnectOptions options = new ConnectOptions();
		options.clientId(client0Id);
		options.will(
				new Message(-1, willTopic, null, message.getBytes(CharsetUtil.UTF_8), MqttQoS.AT_LEAST_ONCE, false));
		options.cleanSession(false);

		MqttClient client0 = new MqttClient("mqtt://localhost:" + Settings.INSTANCE.mqttPort());
		MqttConnectReturnCode ret = client0.connectOptions(options).connect();

		Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, ret);
		Assert.assertTrue(client0.isConnected());

		Assert.assertTrue(Session.NEXUS.get(client0Id).will() != null
				&& Session.NEXUS.get(client0Id).will().topicName().equals(willTopic));

		// ================= client1 creation

		class MessageWrapper {
			private IMessage message = null;

			public IMessage getMessage() {
				return message;
			}

			public void setMessage(IMessage message) {
				this.message = message;
			}
		}

		MessageWrapper wrapper = new MessageWrapper();

		String client1Id = TestUtil.newClientId();
		ConnectOptions options1 = new ConnectOptions();
		options1.clientId(client1Id);

		MqttClient client1 = new MqttClient("mqtt://localhost:" + Settings.INSTANCE.mqttPort());
		ret = client1.connectOptions(options1).receiver(m -> {
			wrapper.setMessage(m);

			synchronized (wrapper) {
				wrapper.notify();
			}
		}).connect();

		client1.subscribe(new MqttTopicSubscription(willTopic, MqttQoS.AT_LEAST_ONCE));

		Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, ret);
		Assert.assertTrue(client1.isConnected());

		Thread.sleep(500); // for client1 connecting & subscribing

		client0.disconnect(false); // abnormal disconnect

		Thread.sleep(500); // for client0's will to null

		synchronized (wrapper) {
			wrapper.wait(5000);
		}

		IMessage will = wrapper.getMessage();

		Assert.assertNull(Session.NEXUS.get(client0Id).will());
		Assert.assertNotNull(will);
		Assert.assertTrue(will.topicName().equals(willTopic));
		Assert.assertNull(Topic.NEXUS.get(will.topicName()).retainedMessage());
		Assert.assertTrue(message.equals(new String(wrapper.getMessage().message())));
	}

	@Test
	public void testWillToNullOnNormalDisconnect() throws Exception {
		String willTopic = "will";
		String message = "ASTALAVISTA";

		String clientId = TestUtil.newClientId();
		ConnectOptions options = new ConnectOptions();
		options.clientId(clientId);
		options.will(
				new Message(-1, willTopic, null, message.getBytes(CharsetUtil.UTF_8), MqttQoS.AT_LEAST_ONCE, false));
		options.cleanSession(false);

		MqttClient client0 = new MqttClient("mqtt://localhost:" + Settings.INSTANCE.mqttPort());
		MqttConnectReturnCode ret = client0.connectOptions(options).connect();

		Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, ret);
		Assert.assertTrue(client0.isConnected());

		Assert.assertTrue(Session.NEXUS.get(clientId).will() != null
				&& Session.NEXUS.get(clientId).will().topicName().equals(willTopic));

		client0.disconnect(true);

		Thread.sleep(100);
		Assert.assertNull(Session.NEXUS.get(clientId).will());
	}
}
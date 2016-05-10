/*
 * Copyright 2016 The Menton Project
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

package net.anyflow.lannister.server;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import net.anyflow.lannister.TestSuite;
import net.anyflow.lannister.TestUtil;
import net.anyflow.lannister.client.MqttClient;
import net.anyflow.lannister.message.ConnectOptions;

public class MqttServerTest {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttServerTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestSuite.setUp();
	}

	@Test
	public void testConnectDisconnect() throws Exception {
		ConnectOptions options = new ConnectOptions();
		options.clientId(TestUtil.newClientId());

		MqttClient client = new MqttClient("mqtt://localhost:1883");
		MqttConnectReturnCode ret = client.connectOptions(options).connect();

		Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, ret);

		Assert.assertTrue(client.isConnected());

		client.disconnect(true);

		Assert.assertFalse(client.isConnected());
	}

	@Test
	public void testSslConnectDisconnect() throws Exception {
		ConnectOptions options = new ConnectOptions();
		options.clientId(TestUtil.newClientId());

		MqttClient client = new MqttClient("mqtts://localhost:8883", true);
		MqttConnectReturnCode ret = client.connectOptions(options).connect();

		Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, ret);

		Assert.assertTrue(client.isConnected());

		client.disconnect(true);

		Assert.assertFalse(client.isConnected());
	}
}
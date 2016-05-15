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

package net.anyflow.lannister.packetreceiver;

import java.util.Date;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.TestSuite;
import net.anyflow.lannister.TestUtil;
import net.anyflow.lannister.client.MqttClient;
import net.anyflow.lannister.message.ConnectOptions;
import net.anyflow.lannister.session.Session;

public class SessionExpiratorTest {
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestSuite.setUp();
	}

	@Test
	public void testHandlerAdded() throws Exception {
		String clientId = TestUtil.newClientId();
		ConnectOptions options = new ConnectOptions();
		options.clientId(clientId);

		MqttClient client = new MqttClient("mqtt://localhost:" + Settings.SELF.mqttPort());
		MqttConnectReturnCode ret = client.connectOptions(options).connect();

		Assert.assertTrue(ret == MqttConnectReturnCode.CONNECTION_ACCEPTED);

		Session session = Session.NEXUS.get(clientId);

		Assert.assertNotNull(session);

		session.setLastIncomingTime(new Date(1L));

		Assert.assertTrue(session.isExpired());

		int expireTimeout = Settings.SELF.getInt("lannister.sessionExpirationHandlerExecutionIntervalSeconds", 1);

		Thread.sleep((expireTimeout + 2) * 1000);

		Assert.assertNull(Session.NEXUS.get(clientId));
	}
}

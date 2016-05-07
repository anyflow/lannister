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

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import io.netty.channel.ChannelId;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.Server;
import net.anyflow.lannister.plugin.Authorization;
import net.anyflow.lannister.plugin.Plugin;
import net.anyflow.lannister.plugin.PluginFactory;
import net.anyflow.lannister.plugin.ServiceStatus;

public class ConnectReceiverTest {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConnectReceiverTest.class);

	@ClassRule
	public static Server server = new Server();

	private int clientIdNo = 0;

	private String newClientId() {
		String clientIdPrefix = "testClientId";

		synchronized (clientIdPrefix) {
			return clientIdPrefix + Integer.toString(clientIdNo++);
		}
	}

	@Test
	public void testNonCleanSessionWithoutClientId() throws Exception {
		// TODO Remove phantom CONNECT message right after CONACK(sent but not
		// received the below client)

		Exception result = null;
		final MqttClient client = new MqttClient("tcp://localhost:1883", "", new MemoryPersistence());

		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setCleanSession(false);

		try {
			client.connect(connOpts);
		}
		catch (MqttException e) {
			logger.error(e.getMessage(), e);
			result = e;
		}

		Assert.assertNotNull(result);
	}

	@Test
	public void testDefaultChannelRead0() throws Exception {
		MqttConnAckMessage ret = executeNormalChannelRead0(newClientId());

		Assert.assertEquals(ret.variableHeader().connectReturnCode(), MqttConnectReturnCode.CONNECTION_ACCEPTED);
	}

	@Test
	public void testCleanSessionWithoutClientId() throws Exception {
		MqttConnAckMessage ret = executeNormalChannelRead0("");

		Assert.assertEquals(ret.variableHeader().connectReturnCode(), MqttConnectReturnCode.CONNECTION_ACCEPTED);
	}

	@Test
	public void testCONNECTION_REFUSED_SERVER_UNAVAILABLE() throws Exception {
		ServiceStatus prev = PluginFactory.serviceStatus(new ServiceStatus() {
			@Override
			public Plugin clone() {
				return this;
			}

			@Override
			public boolean isServiceAvailable() {
				return false;
			}
		});

		MqttConnAckMessage ret = executeNormalChannelRead0(newClientId());

		Assert.assertEquals(ret.variableHeader().connectReturnCode(),
				MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);

		PluginFactory.serviceStatus(prev);
	}

	@Test
	public void testCONNECTION_REFUSED_IDENTIFIER_REJECTED() throws Exception {
		Authorization prev = PluginFactory.authorization(new Authorization() {
			@Override
			public Plugin clone() {
				return this;
			}

			@Override
			public boolean isValid(String clientId) {
				return false;
			}

			@Override
			public boolean isValid(boolean hasUserName, boolean hasPassword, String userName, String password) {
				return true;
			}

			@Override
			public boolean isAuthorized(boolean hasUserName, String username) {
				return true;
			}
		});

		MqttConnAckMessage ret = executeNormalChannelRead0(newClientId());

		Assert.assertEquals(ret.variableHeader().connectReturnCode(),
				MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED);

		PluginFactory.authorization(prev);
	}

	@Test
	public void testCONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD() throws Exception {
		Authorization prev = PluginFactory.authorization(new Authorization() {
			@Override
			public Plugin clone() {
				return this;
			}

			@Override
			public boolean isValid(String clientId) {
				return true;
			}

			@Override
			public boolean isValid(boolean hasUserName, boolean hasPassword, String userName, String password) {
				return false;
			}

			@Override
			public boolean isAuthorized(boolean hasUserName, String username) {
				return true;
			}
		});

		MqttConnAckMessage ret = executeNormalChannelRead0(newClientId());

		Assert.assertEquals(ret.variableHeader().connectReturnCode(),
				MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);

		PluginFactory.authorization(prev);
	}

	@Test
	public void testCONNECTION_REFUSED_NOT_AUTHORIZED() throws Exception {
		Authorization prev = PluginFactory.authorization(new Authorization() {
			@Override
			public Plugin clone() {
				return this;
			}

			@Override
			public boolean isValid(String clientId) {
				return true;
			}

			@Override
			public boolean isValid(boolean hasUserName, boolean hasPassword, String userName, String password) {
				return true;
			}

			@Override
			public boolean isAuthorized(boolean hasUserName, String username) {
				return false;
			}
		});

		MqttConnAckMessage ret = executeNormalChannelRead0(newClientId());

		Assert.assertEquals(ret.variableHeader().connectReturnCode(),
				MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED);

		PluginFactory.authorization(prev);
	}

	private MqttConnAckMessage executeNormalChannelRead0(String clientId) throws Exception {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false,
				10);
		MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader("MQTT", 4, true, true, true, 0, true,
				true, 60);
		MqttConnectPayload payload = new MqttConnectPayload(clientId, "willtopic", "willmessage", "username",
				"password");

		MqttConnectMessage msg = new MqttConnectMessage(fixedHeader, variableHeader, payload);

		EmbeddedChannel channel = new EmbeddedChannel(new ChannelId() {
			private static final long serialVersionUID = 3931333967922160660L;

			@Override
			public int compareTo(ChannelId o) {
				return this.asLongText().equals(o.asLongText()) ? 0 : 1;
			}

			@Override
			public String asShortText() {
				return clientId;
			}

			@Override
			public String asLongText() {
				return clientId;
			}
		}, new ConnectReceiver());

		channel.writeInbound(msg);

		return channel.readOutbound();
	}
}
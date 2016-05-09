package net.anyflow.lannister;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
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
		MqttConnAckMessage ret = client.connectOptions(options).connect();

		Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, ret.variableHeader().connectReturnCode());

		Assert.assertTrue(client.isConnected());

		client.disconnect(true);

		Assert.assertFalse(client.isConnected());
	}

	@Test
	public void testSslConnectDisconnect() throws Exception {
		ConnectOptions options = new ConnectOptions();
		options.clientId(TestUtil.newClientId());

		MqttClient client = new MqttClient("mqtts://localhost:8883", true);
		MqttConnAckMessage ret = client.connectOptions(options).connect();

		Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, ret.variableHeader().connectReturnCode());

		Assert.assertTrue(client.isConnected());

		client.disconnect(true);

		Assert.assertFalse(client.isConnected());
	}
}
package net.anyflow.lannister.packetreceiver;

import java.util.Date;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
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

		MqttClient client = new MqttClient("mqtt://localhost:1883");
		MqttConnectReturnCode ret = client.connectOptions(options).connect();

		Assert.assertTrue(ret == MqttConnectReturnCode.CONNECTION_ACCEPTED);

		Session session = Session.NEXUS.get(clientId);

		Assert.assertNotNull(session);

		session.setLastIncomingTime(new Date(1L));

		Assert.assertTrue(session.isExpired());

		Thread.sleep((TestSuite.SESSION_EXIRE_TIMEOUT + 1) * 1000);

		Assert.assertNull(Session.NEXUS.get(clientId));
	}
}

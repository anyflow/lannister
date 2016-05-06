package net.anyflow.lannister.packetreceiver;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

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

public class ConnectReceiverTest {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConnectReceiverTest.class);

	@ClassRule
	public static Server server = new Server();

	@Test
	public void testNonCleanSessionWithoutClientId() throws Exception {
		logger.debug("testNonCleanSessionWithoutClientId() started");

		InterruptedException result = null;
		final MqttClient client = new MqttClient("tcp://localhost:1883", "", new MemoryPersistence());

		MqttConnectOptions connOpts = new MqttConnectOptions();
		connOpts.setCleanSession(false);

		final Lock lock = new ReentrantLock();
		final Condition condition = lock.newCondition();
		client.setCallback(new MqttCallback() {
			@Override
			public void connectionLost(Throwable cause) {
				logger.error(cause.getMessage(), cause);
				condition.signal();
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				// Do Nothing
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				// Do nothing
			}
		});

		lock.lock();
		try {
			client.connect(connOpts);
		}
		finally {
			lock.unlock();
		}

		Assert.assertNull(result);
		logger.debug("testNonCleanSessionWithoutClientId() finished");
	}

	@Test
	public void testCleanSessionWithoutClientId() throws Exception {
		logger.debug("testCleanSessionWithoutClientId() started");

		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false,
				10);
		MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader("MQTT", 4, true, true, true, 0, true,
				true, 60);
		MqttConnectPayload payload = new MqttConnectPayload("", "willtopic", "willmessage", "username", "password");

		MqttConnectMessage msg = new MqttConnectMessage(fixedHeader, variableHeader, payload);

		EmbeddedChannel channel = new EmbeddedChannel(new ConnectReceiver());
		channel.writeInbound(msg);

		MqttConnAckMessage ret = channel.readOutbound();

		Assert.assertEquals(ret.variableHeader().connectReturnCode(), MqttConnectReturnCode.CONNECTION_ACCEPTED);

		logger.debug("testCleanSessionWithoutClientId() finished");
	}
}

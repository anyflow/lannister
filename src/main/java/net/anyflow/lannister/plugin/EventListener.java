package net.anyflow.lannister.plugin;

import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;

public interface EventListener extends Plugin {
	void connectMessageReceived(final MqttConnectMessage msg);

	void connAckMessageSent(final MqttConnAckMessage msg);
}
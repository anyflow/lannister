package net.anyflow.lannister.messagehandler;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.session.Message;

public class MessageFactory {
	public static MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_LEAST_ONCE, false,
				2);
		MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);

		return new MqttConnAckMessage(fixedHeader, variableHeader);
	}

	public static MqttPublishMessage publish(Message message, boolean isDuplicated, MqttQoS qos, boolean isRetain,
			int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, isDuplicated, qos, isRetain,
				7 + message.message().length);

		MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(message.topicName(), messageId);

		return new MqttPublishMessage(fixedHeader, variableHeader, Unpooled.wrappedBuffer(message.message()));
	}
}
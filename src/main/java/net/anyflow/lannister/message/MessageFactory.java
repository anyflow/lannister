package net.anyflow.lannister.message;

import java.util.List;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;

public class MessageFactory {
	public static MqttConnAckMessage connack(MqttConnectReturnCode returnCode, boolean sessionPresent) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false,
				2);
		MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);

		return new MqttConnAckMessage(fixedHeader, variableHeader);
	}

	public static MqttPublishMessage publish(Message message, boolean isDup) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, isDup, message.qos(),
				message.isRetain(), 7 + message.message().length);

		MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(message.topicName(), message.id());

		return new MqttPublishMessage(fixedHeader, variableHeader, Unpooled.wrappedBuffer(message.message()));
	}

	public static MqttPubAckMessage puback(int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false,
				2);
		MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);

		return new MqttPubAckMessage(fixedHeader, variableHeader);
	}

	public static MqttMessage pubrec(int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false,
				2);
		MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);

		return new MqttMessage(fixedHeader, variableHeader);
	}

	public static MqttMessage pingresp() {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false,
				0);

		return new MqttMessage(fixedHeader);
	}

	public static MqttSubAckMessage suback(int messageId, List<Integer> grantedQoSLevels) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false,
				2 + grantedQoSLevels.size());
		MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);
		MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoSLevels);

		return new MqttSubAckMessage(fixedHeader, variableHeader, payload);
	}

	public static MqttUnsubAckMessage unsuback(int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false,
				2);
		MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);

		return new MqttUnsubAckMessage(fixedHeader, variableHeader);
	}
}

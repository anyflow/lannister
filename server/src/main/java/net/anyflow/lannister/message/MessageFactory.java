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

package net.anyflow.lannister.message;

import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
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
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.util.CharsetUtil;
import net.anyflow.lannister.plugin.IMessage;

public class MessageFactory {
	public static MqttConnectMessage connect(ConnectOptions options) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false,
				10);
		MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(options.version().protocolName(),
				options.version().protocolLevel(), options.userName() != null, options.password() != null,
				options.will() == null ? false : options.will().isRetain(),
				options.will() == null ? 0 : options.will().qos().value(), options.will() != null,
				options.cleanSession(), options.keepAliveTimeSeconds());

		MqttConnectPayload payload = new MqttConnectPayload(Strings.nullToEmpty(options.clientId()),
				options.will() == null ? "" : options.will().topicName(),
				options.will() == null ? "" : new String(options.will().message()),
				Strings.nullToEmpty(options.userName()), Strings.nullToEmpty(options.password()));

		return new MqttConnectMessage(fixedHeader, variableHeader, payload);
	}

	public static MqttConnAckMessage connack(MqttConnectReturnCode returnCode, boolean sessionPresent) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false,
				2);
		MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);

		return new MqttConnAckMessage(fixedHeader, variableHeader);
	}

	public static MqttPublishMessage publish(IMessage message, boolean isDup) {
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

	public static MqttMessage pubrel(int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false,
				2);
		MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);

		return new MqttMessage(fixedHeader, variableHeader);
	}

	public static MqttMessage pubcomp(int messageId) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false,
				2);
		MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);

		return new MqttMessage(fixedHeader, variableHeader);
	}

	public static MqttMessage pingresp() {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false,
				0);

		return new MqttMessage(fixedHeader);
	}

	public static MqttSubscribeMessage subscribe(int messageId, MqttTopicSubscription... topicSubscriptions) {
		int topicNameSize = 0;
		int topicCount = topicSubscriptions.length;

		for (MqttTopicSubscription item : topicSubscriptions) {
			topicNameSize += item.topicName().getBytes(CharsetUtil.UTF_8).length;
		}

		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE,
				false, 2 + topicNameSize + topicCount);
		MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);
		MqttSubscribePayload payload = new MqttSubscribePayload(Lists.newArrayList(topicSubscriptions));

		return new MqttSubscribeMessage(fixedHeader, variableHeader, payload);
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

	public static MqttMessage disconnect() {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE,
				false, 2);

		return new MqttMessage(fixedHeader);
	}
}
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

package net.anyflow.lannister.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.MessageFactory;

public class MqttPacketReceiver extends SimpleChannelInboundHandler<MqttMessage> {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttPacketReceiver.class);

	private final MqttClient client;
	private final MessageReceiver receiver;
	private final SharedObject sharedObject;

	protected MqttPacketReceiver(MqttClient client, MessageReceiver receiver, SharedObject sharedObject) {
		this.client = client;
		this.receiver = receiver;
		this.sharedObject = sharedObject;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
		switch (msg.fixedHeader().messageType()) {
		case PUBLISH:
			if (receiver != null) {
				receiver.messageReceived(Message.newMessage((MqttPublishMessage) msg, client.clientId()));
			}

			int messageId = ((MqttPublishMessage) msg).variableHeader().messageId();
			if (((MqttPublishMessage) msg).fixedHeader().qosLevel() == MqttQoS.AT_LEAST_ONCE) {
				client.send(MessageFactory.puback(messageId));
			}
			else if (((MqttPublishMessage) msg).fixedHeader().qosLevel() == MqttQoS.EXACTLY_ONCE) {
				client.send(MessageFactory.pubrec(messageId));
			}
			break;

		case CONNACK:
			sharedObject.receivedMessage(msg);

			synchronized (sharedObject.locker()) {
				sharedObject.locker().notify();
			}
			break;

		case PUBREC:
			client.send(MessageFactory.pubrel(((MqttMessageIdVariableHeader) msg.variableHeader()).messageId()));
			break;

		case SUBACK:
		case PUBACK:
		case PUBCOMP:
		default:
			break;
		}
	}
}
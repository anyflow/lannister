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

import com.google.common.base.Strings;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttIdentifierRejectedException;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttUnacceptableProtocolVersionException;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.MessageFactory;
import net.anyflow.lannister.plugin.Authorization;
import net.anyflow.lannister.plugin.EventListener;
import net.anyflow.lannister.plugin.PluginFactory;
import net.anyflow.lannister.plugin.ServiceStatus;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;

public class ConnectReceiver extends SimpleChannelInboundHandler<MqttConnectMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConnectReceiver.class);

	private final EventListener eventListener = PluginFactory.eventListener();
	private final ServiceStatus serviceStatus = PluginFactory.serviceStatus();
	private final Authorization auth = PluginFactory.authorization();

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttConnectMessage msg) throws Exception {
		logger.debug("packet incoming : {}", msg.toString());

		eventListener.connectMessageReceived(msg);

		Session session = Session.NEXUS.get(ctx.channel().id());
		if (session != null) {
			session.dispose(true); // [MQTT-3.1.0-2]
			return;
		}

		boolean cleanSession = msg.variableHeader().isCleanSession();
		String clientId = msg.payload().clientIdentifier();

		if (Strings.isNullOrEmpty(clientId)) {
			if (cleanSession) {
				clientId = Settings.SELF.getProperty("mqtt.defaultClientId", "lannisterDefaultClientId"); // [MQTT-3.1.3-6],[MQTT-3.1.3-7]
			}
			else {
				sendNoneAcceptMessage(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false); // [MQTT-3.1.3-8],[MQTT-3.2.2-4]
				return;
			}
		}

		MqttConnectReturnCode returnCode = filterPlugins(msg);
		if (returnCode != MqttConnectReturnCode.CONNECTION_ACCEPTED) {
			sendNoneAcceptMessage(ctx, returnCode, false); // [MQTT-3.2.2-4]
			return;
		}

		// TODO [MQTT-3.1.2-3] handling Reserved Flag, but netty variable header
		// doesn't have it

		session = Session.NEXUS.get(clientId); // [MQTT-3.1.2-4]
		boolean sessionPresent = !cleanSession && session != null; // [MQTT-3.2.2-1],[MQTT-3.2.2-2],[MQTT-3.2.2-3]

		if (cleanSession) {
			if (session != null) {
				session.dispose(false);
			}
			session = new Session(clientId, msg.variableHeader().keepAliveTimeSeconds(), cleanSession,
					newWill(clientId, msg)); // [MQTT-3.1.2-6]
		}
		else if (session == null) { // [MQTT-3.1.2-4]
			session = new Session(clientId, msg.variableHeader().keepAliveTimeSeconds(), cleanSession,
					newWill(clientId, msg)); // [MQTT-3.1.2-6]

		}
		Session.NEXUS.put(session, ctx);

		if (session.will() != null) {
			Topic topic = Topic.NEXUS.get(session.will().topicName());
			if (topic == null) {
				topic = new Topic(session.will().topicName());
				Topic.put(topic);
			}

			if (session.will().isRetain()) { // [MQTT-3.1.2-16],[MQTT-3.1.2-17]
				topic.setRetainedMessage(session.will().message().length > 0 ? session.will() : null);
			}
		}

		final Session sessionFinal = session;
		final MqttConnAckMessage acceptMsg = MessageFactory.connack(returnCode, sessionPresent);

		session.send(acceptMsg).addListener(f -> {
			eventListener.connAckMessageSent(acceptMsg);

			if (!sessionFinal.cleanSession()) {
				sessionFinal.completeRemainedMessages(); // [MQTT-4.4.0-1]
			}
		});
	}

	private Message newWill(String clientId, MqttConnectMessage conn) {
		if (conn.variableHeader().isWillFlag() == false) { return null; } // [MQTT-3.1.2-12]

		return new Message(-1, conn.payload().willTopic(), clientId, conn.payload().willMessage().getBytes(),
				MqttQoS.valueOf(conn.variableHeader().willQos()), conn.variableHeader().isWillRetain());
	}

	private MqttConnectReturnCode filterPlugins(MqttConnectMessage msg) {
		if (serviceStatus.isServiceAvailable() == false) {
			return MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
		}
		else if (auth.isValid(msg.payload().clientIdentifier()) == false) {
			return MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED; // [MQTT-3.1.3-9]
		}
		else if (auth.isValid(msg.variableHeader().hasUserName(), msg.variableHeader().hasPassword(),
				msg.payload().userName(), msg.payload().password()) == false) {
			return MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;
		}
		else if (auth.isAuthorized(msg.variableHeader().hasUserName(),
				msg.payload().userName()) == false) { return MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED; }

		return MqttConnectReturnCode.CONNECTION_ACCEPTED;
	}

	private void sendNoneAcceptMessage(ChannelHandlerContext ctx, MqttConnectReturnCode returnCode,
			boolean sessionPresent) {
		MqttConnAckMessage msg = MessageFactory.connack(returnCode, sessionPresent);

		ctx.channel().writeAndFlush(msg).addListener(f -> {
			logger.debug("packet outgoing [{}]", msg);

			eventListener.connAckMessageSent(msg);
			ctx.channel().disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-3.2.2-5]
		});
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getMessage(), cause);

		MqttConnectReturnCode returnCode;

		if (cause instanceof MqttIdentifierRejectedException) {
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
		}
		else if (cause instanceof IllegalArgumentException && cause.getMessage().contains("invalid QoS")) {
			// [MQTT-3.1.2-2]
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
		}
		else if (cause instanceof IllegalArgumentException && cause.getMessage().contains(" is unknown mqtt version")) {
			// [MQTT-3.1.2-2]
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
		}
		else if (cause instanceof MqttUnacceptableProtocolVersionException) {
			// [MQTT-3.1.2-2]
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
		}
		else {
			super.exceptionCaught(ctx, cause);
			return;
		}

		sendNoneAcceptMessage(ctx, returnCode, true); // [MQTT-3.2.2-4]
	}
}
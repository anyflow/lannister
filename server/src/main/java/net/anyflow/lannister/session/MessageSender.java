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

package net.anyflow.lannister.session;

import java.util.Date;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.Statistics;
import net.anyflow.lannister.message.InboundMessageStatus;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.packetreceiver.MqttMessageFactory;
import net.anyflow.lannister.topic.Topic;

public class MessageSender {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageSender.class);

	private final static int RESPONSE_TIMEOUT_SECONDS = Settings.INSTANCE.getInt("mqttserver.responseTimeoutSeconds",
			60);

	private final Session session;

	protected MessageSender(Session session) {
		this.session = session;
	}

	protected void send(MqttMessage message, GenericFutureListener<? extends Future<? super Void>> completeListener) {
		if (!session.isConnected(true)) {
			logger.error("Message is not sent - Channel is inactive or out of the node. [{}]", message);
			return;
		}

		ChannelHandlerContext ctx = Session.NEXUS.channelHandlerContext(session.clientId());

		String log = message.toString();
		ChannelFuture cf = ctx.writeAndFlush(message).addListener(f -> {
			if (f.isSuccess()) {
				logger.debug("packet outgoing [{}]", log);
			}
			else {
				logger.error("packet outgoing failed [{}] {}", log, f.cause());
			}
		});

		if (completeListener != null) {
			cf.addListener(completeListener);
		}
	}

	protected void sendPublish(Topic topic, Message message) {
		logger.debug("event arrived [clientId={}, message={}]", session.clientId(), message);

		if (!session.isConnected(true)) { return; }

		send(MqttMessageFactory.publish(message, false), f -> { // [MQTT-3.3.1-2]
			if (!f.isSuccess()) { return; }

			Statistics.INSTANCE.add(Statistics.Criterion.MESSAGES_PUBLISH_SENT, 1);

			switch (message.qos()) {
			case AT_MOST_ONCE:
				break;

			case AT_LEAST_ONCE:
			case EXACTLY_ONCE:
				OutboundMessageStatus.NEXUS.update(message.id(), session.clientId(),
						OutboundMessageStatus.Status.PUBLISHED);
				break;

			default:
				logger.error("Invalid QoS [QoS={}, clientId={}, topic={}]", message.qos(), session.clientId(),
						message.topicName());
				break;
			}
		});
	}

	protected void completeRemainedMessages() {
		// TODO should be executed in the middle of 'connected' state?

		completeOutboundMessageStatuses();
		completeInboundMessageStatuses();
	}

	private void completeInboundMessageStatuses() {
		ChannelHandlerContext ctx = Session.NEXUS.channelHandlerContext(session.clientId());
		if (ctx == null) { return; }

		ctx.executor().submit(() -> {
			Date now = new Date();

			InboundMessageStatus.NEXUS.keySet().stream().map(key -> InboundMessageStatus.NEXUS.getByKey(key))
					.forEach(messageStatus -> {
						long intervalSeconds = (now.getTime() - messageStatus.updateTime().getTime()) * 1000;
						if (intervalSeconds < RESPONSE_TIMEOUT_SECONDS) { return; }

						Message message = Message.NEXUS.get(messageStatus.key());
						MqttMessage toSend;
						String log;

						switch (messageStatus.status()) {
						case RECEIVED:
						case PUBRECED:
							if (message.qos() == MqttQoS.AT_LEAST_ONCE) {
								toSend = MqttMessageFactory.puback(message.id());
								log = toSend.toString();

								send(toSend, f -> { // [MQTT-2.3.1-6]
									if (!f.isSuccess()) {
										logger.error("packet outgoing failed [{}] {}", log, f.cause());
										return;
									}

									InboundMessageStatus.NEXUS.removeByKey(message.id(), session.clientId());
									logger.debug("Inbound message status REMOVED [clientId={}, messageId={}]",
											session.clientId(), message.id());
								});
							}
							else {
								toSend = MqttMessageFactory.pubrec(message.id());
								log = toSend.toString();

								send(toSend, f -> {
									if (!f.isSuccess()) {
										logger.error("packet outgoing failed [{}] {}", log, f.cause());
										return;
									}

									InboundMessageStatus.NEXUS.update(message.id(), session.clientId(),
											InboundMessageStatus.Status.PUBRECED);
								}); // [MQTT-2.3.1-6]
							}
							break;

						default:
							logger.error(
									"Invalid Inbound Message Status [status={}, clientId={}, topic={}, messageId={}]",
									messageStatus.status(), session.clientId(), message.topicName(), message.id());
							break;
						}
					});
		});
	}

	private void completeOutboundMessageStatuses() {
		ChannelHandlerContext ctx = Session.NEXUS.channelHandlerContext(session.clientId());
		if (ctx == null) { return; }

		ctx.executor().submit(() -> {
			Date now = new Date();

			OutboundMessageStatus.NEXUS.getMessageIdsOf(session.clientId()).stream()
					.map(messageId -> OutboundMessageStatus.NEXUS.getBy(messageId, session.clientId()))
					.forEach(messageStatus -> {
						long intervalSeconds = (now.getTime() - messageStatus.updateTime().getTime()) * 1000;
						if (intervalSeconds < RESPONSE_TIMEOUT_SECONDS) { return; }

						Message message = Message.NEXUS.get(messageStatus.messageKey());
						MqttMessage toSend;
						String log;

						message.qos(messageStatus.qos());
						message.id(messageStatus.messageId()); // [MQTT-2.3.1-4]
						message.setRetain(false); // [MQTT-3.3.1-9]

						switch (messageStatus.status()) {
						case TO_PUBLISH:
						case PUBLISHED:
							toSend = MqttMessageFactory.publish(message,
									messageStatus.status() == OutboundMessageStatus.Status.PUBLISHED);
							log = toSend.toString();
							send(toSend, f -> { // [MQTT-3.3.1-1]
								if (!f.isSuccess()) {
									logger.error("packet outgoing failed [{}] {}", log, f.cause());
									return;
								}

								OutboundMessageStatus.NEXUS.update(message.id(), session.clientId(),
										OutboundMessageStatus.Status.PUBLISHED);
								Statistics.INSTANCE.add(Statistics.Criterion.MESSAGES_PUBLISH_SENT, 1);
							});
							break;

						case PUBRECED:
							send(MqttMessageFactory.pubrel(message.id()), null);
							break;

						default:
							logger.error(
									"Invalid Outbound Message Status [status={}, clientId={}, topic={}, messageId={}]",
									messageStatus.status(), session.clientId(), message.topicName(), message.id());
							break;
						}
					});
		});
	}
}
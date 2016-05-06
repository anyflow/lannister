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
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.Lists;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.admin.command.MessageFilter;
import net.anyflow.lannister.admin.command.SessionsFilter;
import net.anyflow.lannister.admin.command.TopicsFilter;
import net.anyflow.lannister.message.InboundMessageStatus;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.MessageFactory;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicSubscription;
import net.anyflow.lannister.topic.Topics;

public class MessageSender {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageSender.class);

	private final static List<MessageFilter> FILTERS = Lists.newArrayList(new SessionsFilter(), new TopicsFilter());
	private final static int RESPONSE_TIMEOUT_SECONDS = Settings.SELF.getInt("lannister.responseTimeoutSeconds", 60);

	private final Session session;

	protected MessageSender(Session session) {
		this.session = session;
	}

	protected static MqttQoS adjustQoS(MqttQoS subscriptionQos, MqttQoS publishQos) {
		return subscriptionQos.value() <= publishQos.value() ? subscriptionQos : publishQos;
	}

	protected ChannelFuture send(MqttMessage message) {
		if (session.isConnected() == false) {
			logger.error("Message is not sent - Channel is inactive [{}]", message);
			return null;
		}

		ChannelHandlerContext ctx = Session.NEXUS.channelHandlerContext(session.clientId());

		return ctx.writeAndFlush(message).addListener(f -> {
			logger.debug("packet outgoing [{}]", message);
		});
	}

	protected void sendPublish(Topic topic, Message message, boolean isRetain) {
		logger.debug("event arrived [clientId={}, message={}, isRetain={}]", session.clientId(), message, isRetain);

		// TODO what if returned topicSubscriptions are multiple?
		TopicSubscription subscription = session.matches(message.topicName()).findAny().orElse(null);
		assert subscription != null;

		message.setId(session.nextMessageId());
		message.setRetain(isRetain);
		message.setQos(adjustQoS(subscription.qos(), message.qos()));

		if (message.qos() != MqttQoS.AT_MOST_ONCE) {
			Topic.NEXUS.get(message.topicName()).subscribers().get(session.clientId()).addOutboundMessageStatus(
					message.id(), message.key(), OutboundMessageStatus.Status.TO_PUBLISH, message.qos());
		}

		executefilters(message);

		send(MessageFactory.publish(message, false)).addListener(f -> {
			switch (message.qos()) {
			case AT_MOST_ONCE:
				break;

			case AT_LEAST_ONCE:
			case EXACTLY_ONCE:
				topic.subscribers().get(session.clientId()).setOutboundMessageStatus(message.id(),
						OutboundMessageStatus.Status.PUBLISHED);
				break;

			default:
				logger.error("Invalid QoS [QoS={}, clientId={}, topic={}]", message.qos(), session.clientId(),
						message.topicName());
				break;
			}
		});
	}

	private static void executefilters(Message message) {
		for (MessageFilter filter : FILTERS) {
			filter.execute(message);
		}
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

			Stream<InboundMessageStatus> statuses = Topic.NEXUS.map().values().stream()
					.map(t -> t.inboundMessageStatuses()).flatMap(t -> t.values().stream());

			statuses.forEach(s -> {
				long intervalSeconds = (now.getTime() - s.updateTime().getTime()) * 1000;
				if (intervalSeconds < RESPONSE_TIMEOUT_SECONDS) { return; }

				Topic topic = Topic.NEXUS.get(s.clientId(), s.messageId(), Topics.ClientType.PUBLISHER);
				Message message = topic.messages().get(s.messageId());

				switch (s.status()) {
				case RECEIVED:
				case PUBRECED:
					if (message.qos() == MqttQoS.AT_LEAST_ONCE) {
						session.send(MessageFactory.puback(message.id())).addListener(f -> {
							topic.removeInboundMessageStatus(session.clientId(), message.id());
							logger.debug("Inbound message status REMOVED [clientId={}, messageId={}]",
									session.clientId(), message.id());
						});
					}
					else {
						session.send(MessageFactory.pubrec(message.id()))
								.addListener(f -> topic.setInboundMessageStatus(session.clientId(), message.id(),
										InboundMessageStatus.Status.PUBRECED));
					}
					break;

				default:
					logger.error("Invalid Outbound Message Status [status={}, clientId={}, topic={}, messageId={}]",
							s.status(), session.clientId(), message.topicName(), message.id());
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

			Stream<OutboundMessageStatus> statuses = Topic.NEXUS.map().values().parallelStream()
					.filter(t -> t.subscribers().containsKey(session.clientId()))
					.map(t -> t.subscribers().get(session.clientId())).map(s -> s.outboundMessageStatuses())
					.flatMap(s -> s.values().stream());

			statuses.forEach(s -> {
				long intervalSeconds = (now.getTime() - s.updateTime().getTime()) * 1000;
				if (intervalSeconds < RESPONSE_TIMEOUT_SECONDS) { return; }

				Topic topic = Topic.NEXUS.get(s.clientId(), s.messageId(), Topics.ClientType.SUBSCRIBER);
				Message message = topic.messages().get(s.inboundMessageKey());

				message.setQos(s.qos());
				message.setId(s.messageId());
				message.setRetain(false); // [MQTT-3.3.1-9]

				switch (s.status()) {
				case TO_PUBLISH:
				case PUBLISHED:
					send(MessageFactory.publish(message, s.status() == OutboundMessageStatus.Status.PUBLISHED));
					break;

				case PUBRECED:
					send(MessageFactory.pubrel(message.id()));
					break;

				default:
					logger.error("Invalid Outbound Message Status [status={}, clientId={}, topic={}, messageId={}]",
							s.status(), session.clientId(), message.topicName(), message.id());
					break;
				}
			});
		});
	}
}
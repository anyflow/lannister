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

import java.util.Date;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import net.anyflow.lannister.AbnormalDisconnectEventArgs;
import net.anyflow.lannister.Statistics;
import net.anyflow.lannister.message.InboundMessageStatus;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.MessageFactory;
import net.anyflow.lannister.plugin.DisconnectEventListener;
import net.anyflow.lannister.plugin.IMessage;
import net.anyflow.lannister.plugin.Plugins;
import net.anyflow.lannister.plugin.PublishEventArgs;
import net.anyflow.lannister.plugin.PublishEventListener;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicMatcher;

public class PublishReceiver extends SimpleChannelInboundHandler<MqttPublishMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PublishReceiver.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttPublishMessage msg) throws Exception {
		logger.debug("packet incoming [message={}]", msg.toString());
		Statistics.INSTANCE.add(Statistics.Criterion.MESSAGES_PUBLISH_RECEIVED, 1);

		Session session = Session.NEXUS.get(ctx.channel().id());
		if (session == null) {
			logger.error("None exist session message [message={}]", msg.toString());

			ctx.channel().disconnect().addListener(ChannelFutureListener.CLOSE).addListener(fs -> // [MQTT-4.8.0-1]
			Plugins.INSTANCE.get(DisconnectEventListener.class).disconnected(new AbnormalDisconnectEventArgs()));
			return;
		}

		session.setLastIncomingTime(new Date());

		if (!TopicMatcher.isValid(msg.variableHeader().topicName(), false)) {
			session.dispose(true);
			return;
		}

		Message message = Message.newMessage(session.clientId(), msg);

		if (!Plugins.INSTANCE.get(PublishEventListener.class).allowPublish(new PublishEventArgs() {
			@Override
			public IMessage message() {
				return message;
			}
		})) {
			session.dispose(true);
			return;
		}

		// TODO What to do when sender re-publish message corrensponds to
		// unacked status?

		// TODO Until it has received the corresponding PUBREL packet, the
		// Receiver MUST acknowledge any subsequent PUBLISH packet with the same
		// Packet Identifier by sending a PUBREC. It MUST NOT cause duplicate
		// messages to be delivered to any onward recipients in this
		// case.[MQTT-4.3.3-2].

		final Topic topic = Topic.NEXUS.prepare(message);

		if (message.isRetain()) {// else do nothing [MQTT-3.3.1-12]
			topic.setRetainedMessage(message); // [MQTT-3.3.1-5]
		}

		message.setRetain(false);

		topic.publish(message);

		switch (msg.fixedHeader().qosLevel()) {
		case AT_MOST_ONCE:
			return; // QoS 0 do not send any acknowledge packet [MQTT-3.3.4-1]

		case AT_LEAST_ONCE:
			session.send(MessageFactory.puback(msg.variableHeader().messageId())).addListener(
					f -> topic.removeInboundMessageStatus(session.clientId(), msg.variableHeader().messageId())); // [MQTT-3.3.4-1],[MQTT-2.3.1-6]
			logger.debug("Inbound message status REMOVED [clientId={}, messageId={}]", session.clientId(),
					msg.variableHeader().messageId());
			return;

		case EXACTLY_ONCE:
			session.send(MessageFactory.pubrec(msg.variableHeader().messageId()))
					.addListener(f -> topic.setInboundMessageStatus(session.clientId(),
							msg.variableHeader().messageId(), InboundMessageStatus.Status.PUBRECED)); // [MQTT-3.3.4-1],[MQTT-2.3.1-6]
			return;

		default:
			session.dispose(true); // [MQTT-3.3.1-4]
			return;
		}
	}
}
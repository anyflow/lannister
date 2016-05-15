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
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.plugin.DeliveredEventArgs;
import net.anyflow.lannister.plugin.DeliveredEventListener;
import net.anyflow.lannister.plugin.Plugins;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicSubscriber;
import net.anyflow.lannister.topic.Topics.ClientType;

public class PubAckReceiver extends SimpleChannelInboundHandler<MqttPubAckMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PubAckReceiver.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttPubAckMessage msg) throws Exception {
		logger.debug("packet incoming [message={}]", msg.toString());

		Session session = Session.NEXUS.get(ctx.channel().id());
		if (session == null) {
			logger.error("None exist session message [message={}]", msg.toString());
			ctx.disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-4.8.0-1]
			return;
		}

		session.setLastIncomingTime(new Date());

		String clientId = session.clientId();
		int messageId = msg.variableHeader().messageId();

		Topic topic = Topic.NEXUS.get(clientId, messageId, ClientType.SUBSCRIBER);
		if (topic == null) {
			logger.error("Topic does not exist [clientId={}, messageId={}]", clientId, messageId);
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}

		final TopicSubscriber topicSubscriber = topic.subscribers().get(clientId);

		OutboundMessageStatus status = topicSubscriber.outboundMessageStatuses().get(messageId);

		if (status != null) {
			Plugins.SELF.get(DeliveredEventListener.class).delivered(new DeliveredEventArgs() {
				@Override
				public String clientId() {
					return clientId;
				}

				@Override
				public int messageId() {
					return messageId;
				}
			});
		}

		topicSubscriber.removeOutboundMessageStatus(messageId);
		logger.debug("Outbound message status REMOVED [clientId={}, messageId={}]", clientId, messageId);
	}
}
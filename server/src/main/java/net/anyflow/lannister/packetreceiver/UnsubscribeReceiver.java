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
import java.util.List;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import net.anyflow.lannister.AbnormalDisconnectEventArgs;
import net.anyflow.lannister.message.MessageFactory;
import net.anyflow.lannister.plugin.DisconnectEventListener;
import net.anyflow.lannister.plugin.Plugins;
import net.anyflow.lannister.plugin.UnsubscribeEventArgs;
import net.anyflow.lannister.plugin.UnsubscribeEventListener;
import net.anyflow.lannister.session.Session;

public class UnsubscribeReceiver extends SimpleChannelInboundHandler<MqttUnsubscribeMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UnsubscribeReceiver.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg) throws Exception {
		logger.debug("packet incoming [message={}]", msg.toString());

		Session session = Session.NEXUS.get(ctx.channel().id());
		if (session == null) {
			logger.error("None exist session message [message={}]", msg.toString());

			ctx.channel().disconnect().addListener(ChannelFutureListener.CLOSE).addListener(fs -> // [MQTT-4.8.0-1]
			Plugins.INSTANCE.get(DisconnectEventListener.class).disconnected(new AbnormalDisconnectEventArgs()));
			return;
		}

		session.setLastIncomingTime(new Date());

		List<String> topicFilters = msg.payload().topics();

		if (topicFilters == null || topicFilters.isEmpty()) {
			session.dispose(true); // [MQTT-4.8.0-1]
			return;
		}

		topicFilters.stream().forEach(tf -> session.removeTopicSubscription(tf));

		Plugins.INSTANCE.get(UnsubscribeEventListener.class).unsubscribed(new UnsubscribeEventArgs() {
			@Override
			public String clientId() {
				return session.clientId();
			}

			@Override
			public List<String> topicFilters() {
				return topicFilters;
			}
		});

		session.send(MessageFactory.unsuback(msg.variableHeader().messageId()), null); // [MQTT-2.3.1-7],[MQTT-3.10.4-4],[MQTT-3.10.4-5]
	}
}
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.util.CharsetUtil;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.Statistics;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.MessageFactory;
import net.anyflow.lannister.plugin.DefaultSubscribeEventListener;
import net.anyflow.lannister.plugin.ISession;
import net.anyflow.lannister.plugin.ITopicSubscription;
import net.anyflow.lannister.plugin.Plugins;
import net.anyflow.lannister.plugin.SubscribeEventArgs;
import net.anyflow.lannister.plugin.SubscribeEventListener;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.TopicMatcher;
import net.anyflow.lannister.topic.TopicSubscription;

public class SubscribeReceiver extends SimpleChannelInboundHandler<MqttSubscribeMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SubscribeReceiver.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttSubscribeMessage msg) throws Exception {
		logger.debug("packet incoming [message={}]", msg.toString());

		Session session = Session.NEXUS.get(ctx.channel().id());
		if (session == null) {
			logger.error("None exist session message [message={}]", msg.toString());
			ctx.disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-4.8.0-1]
			return;
		}

		session.setLastIncomingTime(new Date());

		List<MqttTopicSubscription> topicSubs = msg.payload().topicSubscriptions();

		if (topicSubs == null || topicSubs.isEmpty()) {
			session.dispose(true); // [MQTT-4.8.0-1]
			return;
		}

		// TODO multiple sub checking (granted QoS)
		Map.Entry<List<Integer>, Map<String, TopicSubscription>> returns = generateReturns(topicSubs);
		List<Integer> grantedQoss = returns.getKey();
		Map<String, TopicSubscription> topicSubscriptions = returns.getValue();

		if (!executePlugins(session, topicSubscriptions.values())) { return; }

		session.topicSubscriptions().putAll(topicSubscriptions);

		session.send(MessageFactory.suback(msg.variableHeader().messageId(), grantedQoss)); // [MQTT-2.3.1-7],[MQTT-2.3.1-7],[MQTT-3.8.4-1],[MQTT-3.8.4-2]

		sendRetainedMessage(session, topicSubscriptions);

		publishStatic$Sys(session, topicSubscriptions.values());

		// TODO [MQTT-3.3.1-7]
	}

	private void sendRetainedMessage(Session session, Map<String, TopicSubscription> topicSubscriptions) {
		session.topics(topicSubscriptions.values()).forEach(topic -> {
			if (topic.retainedMessage() == null) { return; }

			topic.putMessage(topic.retainedMessage().publisherId(), topic.retainedMessage());

			session.sendPublish(topic, topic.retainedMessage()); // [MQTT-3.3.1-6],[MQTT-3.3.1-8]
		});
	}

	private Map.Entry<List<Integer>, Map<String, TopicSubscription>> generateReturns(
			List<MqttTopicSubscription> topicSubs) {
		List<Integer> grantedQoss = Lists.newArrayList();
		Map<String, TopicSubscription> topicSubscriptions = Maps.newHashMap();

		topicSubs.stream().forEach(topicSub -> {
			if (TopicMatcher.isValid(topicSub.topicName(), true)) {
				TopicSubscription topicSubscription = new TopicSubscription(topicSub.topicName(),
						topicSub.qualityOfService());

				grantedQoss.add(topicSubscription.qos().value());
				topicSubscriptions.put(topicSubscription.topicFilter(), topicSubscription);
			}
			else {
				grantedQoss.add(MqttQoS.FAILURE.value());
			}
		});

		return new Map.Entry<List<Integer>, Map<String, TopicSubscription>>() {
			@Override
			public List<Integer> getKey() {
				return grantedQoss;
			}

			@Override
			public Map<String, TopicSubscription> getValue() {
				return topicSubscriptions;
			}

			@Override
			public Map<String, TopicSubscription> setValue(Map<String, TopicSubscription> value) {
				return null; // Should not be called
			}
		};
	}

	private boolean executePlugins(Session session, Collection<TopicSubscription> topicSubscriptions) {
		SubscribeEventArgs args = new SubscribeEventArgs() {
			@Override
			public List<ITopicSubscription> topicSubscriptions() {
				return Lists.newArrayList(topicSubscriptions);
			}

			@Override
			public ISession session() {
				return session;
			}
		};

		if (!DefaultSubscribeEventListener.SHARED.allowSubscribe(args)) {
			session.dispose(true);
			return false;
		}

		if (!Plugins.SELF.get(SubscribeEventListener.class).allowSubscribe(args)) {
			session.dispose(true);
			return false;
		}

		return true;
	}

	private void publishStatic$Sys(Session session, Collection<TopicSubscription> topicSubscriptions) {
		String requesterId = Settings.SELF.getProperty("lannister.broker.id", "lannister_broker_id");

		topicSubscriptions.stream().forEach(ts -> {
			Statistics.$SYS_STATIC_TOPICS.stream().forEach(t -> {
				if (!TopicMatcher.match(ts.topicFilter(), t)) { return; }

				byte[] msg = Statistics.SELF.getStatic(t).getBytes(CharsetUtil.UTF_8);

				session.send(MessageFactory.publish(new Message(-1, t, requesterId, msg, MqttQoS.AT_MOST_ONCE, false),
						false)).addListener(f -> {
					Statistics.SELF.add(Statistics.Criterion.MESSAGES_PUBLISH_SENT, 1);
				});
			});
		});
	}
}
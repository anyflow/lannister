package net.anyflow.lannister.packetreceiver;

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import net.anyflow.lannister.message.MessageFactory;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.TopicSubscription;

public class SubscribeReceiver extends SimpleChannelInboundHandler<MqttSubscribeMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SubscribeReceiver.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttSubscribeMessage msg) throws Exception {
		logger.debug("packet incoming : {}", msg.toString());

		Session session = Session.NEXUS.get(ctx.channel().id());
		if (session == null) {
			logger.error("None exist session message : {}", msg.toString());
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
		List<Integer> grantedQoss = Lists.newArrayList();
		List<TopicSubscription> topicSubscriptions = Lists.newArrayList();

		topicSubs.stream().filter(topicSub -> {
			if (TopicSubscription.isValid(topicSub.topicName())) { return true; }

			grantedQoss.add(MqttQoS.FAILURE.value());
			return false;
		}).forEach(topicSub -> {
			TopicSubscription topicSubscription = new TopicSubscription(topicSub.topicName(),
					topicSub.qualityOfService());

			session.putTopicSubscription(topicSubscription);

			grantedQoss.add(topicSubscription.qos().value());
			topicSubscriptions.add(topicSubscription);
		});

		session.send(MessageFactory.suback(msg.variableHeader().messageId(), grantedQoss)); // [MQTT-2.3.1-7],[MQTT-3.8.4-1],[MQTT-3.8.4-2]

		session.topics(topicSubscriptions).forEach(topic -> {
			if (topic.retainedMessage() == null) { return; }

			topic.putMessage(topic.retainedMessage().publisherId(), topic.retainedMessage());

			session.sendPublish(topic, topic.retainedMessage(), true); // [MQTT-3.3.1-6],[MQTT-3.3.1-8]
		});

		// TODO [MQTT-3.3.1-7]
	}
}
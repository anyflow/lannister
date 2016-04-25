package net.anyflow.lannister.messagehandler;

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import net.anyflow.lannister.session.Session;

public class MqttSubscribeMessageHandler extends SimpleChannelInboundHandler<MqttSubscribeMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttSubscribeMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttSubscribeMessage msg) throws Exception {
		logger.debug("packet incoming : {}", msg.toString());

		Session session = Session.getByChannelId(ctx.channel().id());
		if (session == null) {
			logger.error("None exist session message : {}", msg.toString());
			ctx.disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-4.8.0-1]
			return;
		}

		session.setLastIncomingTime(new Date());

		List<MqttTopicSubscription> topics = msg.payload().topicSubscriptions();

		if (topics == null || topics.isEmpty()) {
			session.dispose(true); // [MQTT-4.8.0-1]
			return;
		}

		List<Integer> grantedQoss = Lists.newArrayList();

		for (MqttTopicSubscription item : topics) {
			MqttQoS qos = item.qualityOfService();

			if (session.putTopic(item.topicName(), item.qualityOfService()) == false) {
				qos = MqttQoS.FAILURE;
			}

			grantedQoss.add(qos.value());
		}

		session.send(MessageFactory.suback(msg.variableHeader().messageId(), grantedQoss)); // [MQTT-2.3.1-7],[MQTT-3.8.4-1],[MQTT-3.8.4-2]
	}
}
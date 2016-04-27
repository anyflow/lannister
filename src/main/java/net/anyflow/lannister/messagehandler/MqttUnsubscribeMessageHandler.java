package net.anyflow.lannister.messagehandler;

import java.util.Date;
import java.util.List;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import net.anyflow.lannister.session.Session;

public class MqttUnsubscribeMessageHandler extends SimpleChannelInboundHandler<MqttUnsubscribeMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(MqttUnsubscribeMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg) throws Exception {
		logger.debug("packet incoming : {}", msg.toString());

		Session session = Session.getByChannelId(ctx.channel().id());
		if (session == null) {
			logger.error("None exist session message : {}", msg.toString());
			ctx.disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-4.8.0-1]
			return;
		}

		session.setLastIncomingTime(new Date());

		List<String> topicFilters = msg.payload().topics();

		if (topicFilters == null || topicFilters.isEmpty()) {
			session.dispose(true); // [MQTT-4.8.0-1]
			return;
		}

		topicFilters.stream().forEach(tf -> session.removeTopicSubscription(tf));

		session.send(MessageFactory.unsuback(msg.variableHeader().messageId())); // [MQTT-3.10.4-4],[MQTT-3.10.4-5]
	}
}
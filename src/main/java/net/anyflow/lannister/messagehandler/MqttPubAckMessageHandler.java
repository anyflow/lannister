package net.anyflow.lannister.messagehandler;

import java.util.Date;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import net.anyflow.lannister.session.LiveSessions;
import net.anyflow.lannister.session.Session;

public class MqttPubAckMessageHandler extends SimpleChannelInboundHandler<MqttPubAckMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttPubAckMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttPubAckMessage msg) throws Exception {
		logger.debug("packet incoming : {}", msg.toString());

		Session session = LiveSessions.SELF.getByChannelId(ctx.channel().id());
		if (session == null) {
			logger.error("None exist session message : {}", msg.toString());
			return;
		}

		session.setLastIncomingTime(new Date());

		logger.debug("MqttPubAckMessageHandler execution finished.");
	}
}

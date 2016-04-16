package net.anyflow.lannister.messagehandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;

public class MqttPubAckMessageHandler extends SimpleChannelInboundHandler<MqttPubAckMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttPubAckMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttPubAckMessage msg) throws Exception {
		logger.debug(msg.toString());
	}
}

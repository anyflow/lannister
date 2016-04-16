package net.anyflow.lannister.messagehandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;

public class MqttSubscribeMessageHandler extends SimpleChannelInboundHandler<MqttSubscribeMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttSubscribeMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttSubscribeMessage msg) throws Exception {
		logger.debug(msg.toString());
	}
}

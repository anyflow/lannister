package net.anyflow.lannister.messagehandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;

public class MqttUnsubscribeMessageHandler extends SimpleChannelInboundHandler<MqttUnsubscribeMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(MqttUnsubscribeMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg) throws Exception {
		logger.debug(msg.toString());
	}
}

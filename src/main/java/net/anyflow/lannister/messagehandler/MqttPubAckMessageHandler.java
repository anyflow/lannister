package net.anyflow.lannister.messagehandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;

public class MqttPubAckMessageHandler extends SimpleChannelInboundHandler<MqttPubAckMessage> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttPubAckMessage msg) throws Exception {

	}
}

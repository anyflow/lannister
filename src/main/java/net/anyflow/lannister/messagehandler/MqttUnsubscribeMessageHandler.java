package net.anyflow.lannister.messagehandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;

public class MqttUnsubscribeMessageHandler extends SimpleChannelInboundHandler<MqttUnsubscribeMessage> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg) throws Exception {

	}
}

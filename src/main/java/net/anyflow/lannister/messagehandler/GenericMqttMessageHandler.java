package net.anyflow.lannister.messagehandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;

public class GenericMqttMessageHandler extends SimpleChannelInboundHandler<MqttMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GenericMqttMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
		if (msg.decoderResult().isSuccess() == false) {
			logger.error("decoding MQTT message failed : {}", msg.decoderResult().cause().getMessage());
		}
		else {
			logger.debug(msg.toString());
		}
	}
}
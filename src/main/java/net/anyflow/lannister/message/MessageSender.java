package net.anyflow.lannister.message;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

public class MessageSender {

	// TODO send retain message [MQTT-3.3.1-6]
	// TODO retain message flag should be true [MQTT-3.3.1-8]

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageSender.class);

	private final ChannelHandlerContext ctx;

	public MessageSender(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	public static MqttQoS adjustQoS(MqttQoS topicQos, MqttQoS messageQos) {
		return topicQos.value() <= messageQos.value() ? topicQos : messageQos;
	}

	public ChannelFuture send(MqttMessage message) {
		if (ctx == null || ctx.channel().isActive() == false) {
			logger.error("Message is not sent - Channel is inactive : {}", message);
			return null;
		}

		final String log = message.toString();
		return ctx.writeAndFlush(message).addListener(f -> {
			logger.debug("packet outgoing : {}", log);
		});
	}
}
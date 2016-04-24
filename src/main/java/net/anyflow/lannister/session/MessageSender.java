package net.anyflow.lannister.session;

import java.util.Map;
import java.util.function.Consumer;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.messagehandler.MessageFactory;

public class MessageSender {

	// TODO send retain message [MQTT-3.3.1-6]
	// TODO retain message flag should be true [MQTT-3.3.1-8]

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageSender.class);

	private final ChannelHandlerContext ctx;
	private final Map<String, Topic> topics;
	private final Map<Integer, Message> messages;
	private final Synchronizer synchronizer;

	public MessageSender(ChannelHandlerContext ctx, Map<String, Topic> topics, Map<Integer, Message> messages,
			Synchronizer synchronizer) {
		this.ctx = ctx;
		this.topics = topics;
		this.messages = messages;
		this.synchronizer = synchronizer;
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
		return ctx.writeAndFlush(message).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				logger.debug("packet outgoing : {}", log);
			}
		});
	}

	public void publishUnackedMessages() {
		for (String key : topics.keySet()) {
			final Topic topic = topics.get(key);

			messages.keySet().stream().sorted().forEach(new Consumer<Integer>() {
				@Override
				public void accept(Integer t) {
					Message message = messages.get(t);

					message.setQos(adjustQoS(topic.qos(), message.qos()));

					MessageSender.this.send(MessageFactory.publish(message)).addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							message.setSent(true);
							synchronizer.execute();
						}
					});
				}
			});
		}
	}
}
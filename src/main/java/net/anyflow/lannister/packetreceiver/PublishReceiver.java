package net.anyflow.lannister.packetreceiver;

import java.util.Date;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import net.anyflow.lannister.NettyUtil;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.MessageFactory;
import net.anyflow.lannister.message.ReceiverTargetStatus;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;

public class PublishReceiver extends SimpleChannelInboundHandler<MqttPublishMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PublishReceiver.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttPublishMessage msg) throws Exception {
		logger.debug("packet incoming : {}", msg.toString());

		Session session = Session.NEXUS.lives().get(ctx.channel().id());
		if (session == null) {
			logger.error("None exist session message : {}", msg.toString());
			ctx.disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-4.8.0-1]
			return;
		}

		session.setLastIncomingTime(new Date());

		Topic topic = Topic.NEXUS.get(msg.variableHeader().topicName());
		if (topic == null) {
			topic = new Topic(msg.variableHeader().topicName());
			Topic.put(topic);
		}

		Message message = new Message(msg.variableHeader().messageId(), msg.variableHeader().topicName(),
				session.clientId(), NettyUtil.copy(msg.payload()), msg.fixedHeader().qosLevel(),
				msg.fixedHeader().isRetain());

		// [MQTT-3.3.1-10],[MQTT-3.3.1-11]
		if (message.isRetain() && message.message().length > 0) {
			topic.setRetainedMessage(message);
		}
		else {
			// do nothing [MQTT-3.3.1-12]
		}

		topic.publish(session.clientId(), message);

		final Topic topicFinal = topic;

		switch (msg.fixedHeader().qosLevel()) {
		case AT_MOST_ONCE:
			return; // QoS 0 do not send any acknowledge packet [MQTT-3.3.4-1]

		case AT_LEAST_ONCE:
			session.send(MessageFactory.puback(msg.variableHeader().messageId())).addListener(
					f -> topicFinal.removeReceivedMessageStatus(session.clientId(), msg.variableHeader().messageId())); // [MQTT-3.3.4-1],[MQTT-2.3.1-6]
			return;

		case EXACTLY_ONCE:
			session.send(MessageFactory.pubrec(msg.variableHeader().messageId()))
					.addListener(f -> topicFinal.setReceivedMessageStatus(session.clientId(),
							msg.variableHeader().messageId(), ReceiverTargetStatus.TO_COMP)); // [MQTT-3.3.4-1],[MQTT-2.3.1-6]
			return;

		default:
			session.dispose(true); // [MQTT-3.3.1-4]
			return;
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getMessage(), cause);

		if (IllegalArgumentException.class.getName().equals(cause.getClass().getName())
				&& cause.getMessage().contains("invalid QoS")) {
			Session session = Session.NEXUS.lives().get(ctx.channel().id());

			if (session != null) {
				session.dispose(true); // [MQTT-3.3.1-4]
			}
			else {
				ctx.channel().disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-3.3.1-4]
			}
		}
		else {
			super.exceptionCaught(ctx, cause);
		}
	}
}
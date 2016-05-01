package net.anyflow.lannister.packetreceiver;

import java.util.Date;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicSubscriber;
import net.anyflow.lannister.topic.Topics.ClientType;

public class PubAckReceiver extends SimpleChannelInboundHandler<MqttPubAckMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PubAckReceiver.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttPubAckMessage msg) throws Exception {
		logger.debug("packet incoming : {}", msg.toString());

		Session session = Session.NEXUS.get(ctx.channel().id());
		if (session == null) {
			logger.error("None exist session message : {}", msg.toString());
			ctx.disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-4.8.0-1]
			return;
		}

		session.setLastIncomingTime(new Date());

		Topic topic = Topic.NEXUS.get(session.clientId(), msg.variableHeader().messageId(), ClientType.SUBSCRIBER);
		if (topic == null) {
			logger.error("Topic does not exist : [clientId={}, messageId={}]", session.clientId(),
					msg.variableHeader().messageId());
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}

		final TopicSubscriber topicSubscriber = topic.subscribers().get(session.clientId());

		OutboundMessageStatus status = topicSubscriber.sentOutboundMessageStatuses()
				.get(msg.variableHeader().messageId());

		if (status == null) {
			logger.error("No message status to REMOVE(QoS1) : [clientId={}, messageId={}]", session.clientId(),
					msg.variableHeader().messageId());
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}
		if (status.targetStatus() != OutboundMessageStatus.Status.TO_BE_REMOVED) {
			logger.error("Invalid status to REMOVE(QoS1) : [clientId={}, messageId={}, status={}]", session.clientId(),
					msg.variableHeader().messageId(), status);
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}

		topicSubscriber.removeOutboundMessageStatus(msg.variableHeader().messageId());
		logger.debug("Outbound message status REMOVED : [clientId={}, messageId={}]", session.clientId(),
				msg.variableHeader().messageId());
	}
}
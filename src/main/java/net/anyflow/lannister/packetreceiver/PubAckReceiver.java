package net.anyflow.lannister.packetreceiver;

import java.util.Date;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import net.anyflow.lannister.message.SentMessageStatus;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;

public class PubAckReceiver extends SimpleChannelInboundHandler<MqttPubAckMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PubAckReceiver.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttPubAckMessage msg) throws Exception {
		logger.debug("packet incoming : {}", msg.toString());

		Session session = Session.NEXUS.lives().get(ctx.channel().id());
		if (session == null) {
			logger.error("None exist session message : {}", msg.toString());
			ctx.disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-4.8.0-1]
			return;
		}

		session.setLastIncomingTime(new Date());

		SentMessageStatus status = Topic.NEXUS.messageAcked(session.clientId(), msg.variableHeader().messageId());

		if (status == null) {
			logger.error("No message exist to ack : [clientId:{}, messageId:{}]", session.clientId(),
					msg.variableHeader().messageId());
		}
		else {
			logger.debug("message acked : [clientId:{}, messageId:{}]", session.clientId(),
					msg.variableHeader().messageId());
		}
	}
}
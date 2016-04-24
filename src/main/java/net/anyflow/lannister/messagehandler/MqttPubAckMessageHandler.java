package net.anyflow.lannister.messagehandler;

import java.util.Date;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import net.anyflow.lannister.session.Message;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.session.Sessions;

public class MqttPubAckMessageHandler extends SimpleChannelInboundHandler<MqttPubAckMessage> {

	// TODO [DEBUG] first ack is always no message exist

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttPubAckMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttPubAckMessage msg) throws Exception {
		logger.debug("packet incoming : {}", msg.toString());

		Session session = Sessions.SELF.getByChannelId(ctx.channel().id());
		if (session == null) {
			logger.error("None exist session message : {}", msg.toString());
			Sessions.SELF.dispose(session, true); // [MQTT-4.8.0-1]
			return;
		}

		session.setLastIncomingTime(new Date());

		Message message = session.removeMessage(msg.variableHeader().messageId());
		if (message == null) {
			logger.error("No message exist to ack : [clientId:{}, messageId:{}]", session.clientId(),
					msg.variableHeader().messageId());
		}
		else {
			logger.debug("message acked : [clientId:{}, messageId:{}]", session.clientId(),
					msg.variableHeader().messageId());
		}
	}
}
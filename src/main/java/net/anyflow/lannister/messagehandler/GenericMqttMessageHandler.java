package net.anyflow.lannister.messagehandler;

import java.util.Date;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import net.anyflow.lannister.session.Sessions;
import net.anyflow.lannister.session.Session;

public class GenericMqttMessageHandler extends SimpleChannelInboundHandler<MqttMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GenericMqttMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
		if (msg.decoderResult().isSuccess() == false) {
			logger.error("decoding MQTT message failed : {}", msg.decoderResult().cause().getMessage());
		}
		else {
			logger.debug("packet incoming : {}", msg.toString());

			Session session = Sessions.SELF.getByChannelId(ctx.channel().id());
			if (session == null) {
				logger.error("None exist session message : {}", msg.toString());
				Sessions.SELF.dispose(session, true);
				return;
			}

			session.setLastIncomingTime(new Date());

			switch (msg.fixedHeader().messageType()) {
			case DISCONNECT:
				Sessions.SELF.dispose(session, false);
				break;

			case PINGREQ:
				session.send(MessageFactory.pingresp());
				break;

			// PUBREC(5),
			// PUBREL(6),
			// PUBCOMP(7),
			// PINGRESP(13)// never incoming

			default:
				Sessions.SELF.dispose(session, true);
			}
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Session session = Sessions.SELF.getByChannelId(ctx.channel().id());
		if (session == null) {
			logger.debug("session does not exist : [channelId={}]", ctx.channel().id());
			return;
		}
		else {
			Sessions.SELF.dispose(session, true); // abnormal disconnection
														// without DISCONNECT
		}
	}
}
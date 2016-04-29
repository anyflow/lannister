package net.anyflow.lannister.packetreceiver;

import java.util.Date;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import net.anyflow.lannister.session.Session;

public class GenericReceiver extends SimpleChannelInboundHandler<MqttMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GenericReceiver.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
		if (msg.decoderResult().isSuccess() == false) {
			logger.error("decoding MQTT message failed : {}", msg.decoderResult().cause().getMessage());
		}
		else {
			logger.debug("packet incoming : {}", msg.toString());

			Session session = Session.NEXUS.lives().get(ctx.channel().id());
			if (session == null) {
				logger.error("None exist session message : {}", msg.toString());
				ctx.disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-4.8.0-1]
				return;
			}

			session.setLastIncomingTime(new Date());

			switch (msg.fixedHeader().messageType()) {
			case DISCONNECT:
				DisconnectReceiver.SHARED.handle(session);
				return;

			case PINGREQ:
				PingReqReceiver.SHARED.handle(session);
				return;

			case PUBREC:
				PubRecReceiver.SHARED.handle(session, ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId());
				return;

			case PUBREL:
				PubRelReceiver.SHARED.handle(session, ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId());
				return;

			case PUBCOMP:
				PubCompReceiver.SHARED.handle(session,
						((MqttMessageIdVariableHeader) msg.variableHeader()).messageId());
				return;

			default:
				session.dispose(true); // [MQTT-4.8.0-1]
				return;
			}
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Session session = Session.NEXUS.lives().get(ctx.channel().id());
		if (session == null) {
			logger.debug("session does not exist : [channelId={}]", ctx.channel().id());
			return;
		}
		else {
			session.dispose(true); // abnormal disconnection without
									// DISCONNECT [MQTT-4.8.0-1]
		}
	}
}
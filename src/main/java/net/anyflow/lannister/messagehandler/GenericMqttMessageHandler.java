package net.anyflow.lannister.messagehandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.session.SessionNexus;

public class GenericMqttMessageHandler extends SimpleChannelInboundHandler<MqttMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GenericMqttMessageHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
		if (msg.decoderResult().isSuccess() == false) {
			logger.error("decoding MQTT message failed : {}", msg.decoderResult().cause().getMessage());
		}
		else {
			logger.debug(msg.toString());

			Session session = SessionNexus.SELF.getByChannelId(ctx.channel().id().toString());
			if (session == null) {
				logger.error("session does not exist. {}", ctx.channel().id().toString());
				// TODO handing null session
				return;
			}

			switch (msg.fixedHeader().messageType()) {
			case DISCONNECT:
				SessionNexus.SELF.dispose(session);
				break;

			case PINGREQ:
				MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false,
						MqttQoS.AT_LEAST_ONCE, false, 0);

				ctx.channel().writeAndFlush(new MqttMessage(fixedHeader));
				break;

			// PUBREC(5),
			// PUBREL(6),
			// PUBCOMP(7),
			// PINGRESP(13)// never incoming

			default:
				throw new IllegalArgumentException(msg.toString());
			}
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Session session = SessionNexus.SELF.getByChannelId(ctx.channel().id().toString());
		if (session == null) {
			logger.error("session does not exist. {}", ctx.channel().id().toString());
			// TODO handing null session
			return;
		}

		SessionNexus.SELF.dispose(session);
	}
}
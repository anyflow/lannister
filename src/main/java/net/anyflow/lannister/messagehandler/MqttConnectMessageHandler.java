package net.anyflow.lannister.messagehandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.session.SessionNexus;

public class MqttConnectMessageHandler extends SimpleChannelInboundHandler<MqttConnectMessage> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttConnectMessage msg) throws Exception {
		String clientId = msg.payload().clientIdentifier();

		Session session = SessionNexus.SELF.getByClientId(clientId);
		boolean sessionPresent = session != null;
		if (sessionPresent) {
			// TODO handling duplicated session
		}

		// TOOD handling the below
		// CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION((byte) 0X01),
		// CONNECTION_REFUSED_IDENTIFIER_REJECTED((byte) 0x02),
		// CONNECTION_REFUSED_SERVER_UNAVAILABLE((byte) 0x03),
		// CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD((byte) 0x04),
		// CONNECTION_REFUSED_NOT_AUTHORIZED((byte) 0x05);

		session = new Session(ctx, clientId);
		SessionNexus.SELF.put(session);

		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false,
				2);

		MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(
				MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent);

		ctx.channel().writeAndFlush(new MqttConnAckMessage(fixedHeader, variableHeader));
	}
}
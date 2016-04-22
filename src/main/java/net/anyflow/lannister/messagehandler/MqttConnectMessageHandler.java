package net.anyflow.lannister.messagehandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttIdentifierRejectedException;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttUnacceptableProtocolVersionException;
import net.anyflow.lannister.plugin.Authorization;
import net.anyflow.lannister.plugin.EventListener;
import net.anyflow.lannister.plugin.PluginFactory;
import net.anyflow.lannister.plugin.ServiceStatus;
import net.anyflow.lannister.session.LiveSessions;
import net.anyflow.lannister.session.Repository;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.session.Will;

public class MqttConnectMessageHandler extends SimpleChannelInboundHandler<MqttConnectMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttConnectMessageHandler.class);

	EventListener eventListener = (EventListener) (new PluginFactory()).create(EventListener.class);
	ServiceStatus serviceStatus = (ServiceStatus) (new PluginFactory()).create(ServiceStatus.class);
	Authorization auth = (Authorization) (new PluginFactory()).create(Authorization.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttConnectMessage msg) throws Exception {
		logger.debug(msg.toString());

		eventListener.connectMessageReceived(msg);

		String clientId = msg.payload().clientIdentifier();

		MqttConnectReturnCode returnCode = MqttConnectReturnCode.CONNECTION_ACCEPTED;

		if (serviceStatus.isServiceAvailable() == false) {
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
		}
		else if (auth.isValid(msg.payload().clientIdentifier()) == false) {
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
		}
		else if (auth.isValid(msg.variableHeader().hasUserName(), msg.variableHeader().hasPassword(),
				msg.payload().userName(), msg.payload().password()) == false) {
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;
		}
		else if (auth.isAuthorized(msg.variableHeader().hasUserName(), msg.payload().userName()) == false) {
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
		}

		if (returnCode != MqttConnectReturnCode.CONNECTION_ACCEPTED) {
			sendConnAckMessage(ctx, returnCode, false);
			LiveSessions.SELF.dispose(ctx);
			return;
		}

		// TODO [MQTT-3.1.2-3] handling Reserved Flag, but netty variable header
		// doesn't have it

		Session session = LiveSessions.SELF.getByClientId(clientId);
		if (session != null) {
			LiveSessions.SELF.dispose(session); // Dispose prev live session
												// [MQTT-3.1.4-2]
		}

		if (msg.variableHeader().isCleanSession()) {
			Repository.SELF.sessions().remove(clientId);

			session = new Session(ctx, clientId, false); // [MQTT-3.1.2-6]

			LiveSessions.SELF.put(session);
		}
		else {
			session = Repository.SELF.sessions().get(clientId);

			if (session == null) {
				session = new Session(ctx, clientId, true);
			}
			else {
				session.revive(ctx);
			}

			LiveSessions.SELF.put(session);
		}

		if (msg.variableHeader().isWillFlag()) {
			session.setWill(new Will(msg.payload().willTopic(), msg.payload().willMessage(),
					msg.variableHeader().willQos() == 0 ? MqttQoS.AT_MOST_ONCE : MqttQoS.AT_LEAST_ONCE,
					msg.variableHeader().isWillRetain()));
		}

		sendConnAckMessage(ctx, returnCode, false);
	}

	private void sendConnAckMessage(ChannelHandlerContext ctx, MqttConnectReturnCode returnCode,
			boolean sessionPresent) {
		MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_LEAST_ONCE, false,
				2);
		MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);

		MqttConnAckMessage connAckMsg = new MqttConnAckMessage(fixedHeader, variableHeader);

		ctx.channel().writeAndFlush(connAckMsg);

		eventListener.connAckMessageSent(connAckMsg);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getMessage(), cause);

		if (MqttIdentifierRejectedException.class.getName().equals(cause.getClass().getName())) {
			sendConnAckMessage(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false);
			LiveSessions.SELF.dispose(ctx);
		}
		else if (IllegalArgumentException.class.getName().equals(cause.getClass().getName())
				&& cause.getMessage().contains(" is unknown mqtt version")) {
			// [MQTT-3.1.2-2]
			sendConnAckMessage(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, false);

			LiveSessions.SELF.dispose(ctx);
		}
		else if (MqttUnacceptableProtocolVersionException.class.getName().equals(cause.getClass().getName())) {
			// [MQTT-3.1.2-2]
			sendConnAckMessage(ctx, MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, false);
			LiveSessions.SELF.dispose(ctx);
		}
		else {
			super.exceptionCaught(ctx, cause);
		}
	}
}
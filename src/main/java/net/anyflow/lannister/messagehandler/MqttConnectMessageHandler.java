package net.anyflow.lannister.messagehandler;

import com.google.common.base.Strings;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttIdentifierRejectedException;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttUnacceptableProtocolVersionException;
import net.anyflow.lannister.Settings;
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
		logger.debug("MQTT message incoming : {}", msg.toString());

		eventListener.connectMessageReceived(msg);

		String clientId = msg.payload().clientIdentifier();

		Session session = LiveSessions.SELF.getByChannelId(ctx.channel().id());
		if (session != null) {
			LiveSessions.SELF.dispose(session); // [MQTT-3.1.0-2]
			return;
		}

		MqttConnectReturnCode returnCode = MqttConnectReturnCode.CONNECTION_ACCEPTED;

		boolean cleanSession = msg.variableHeader().isCleanSession();

		if (Strings.isNullOrEmpty(clientId)) {
			if (cleanSession == false) {
				returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED; // [MQTT-3.1.3-8]
			}
			else if (Settings.SELF.getBoolean("mqtt.acceptEmptyClientId", false)) { // [MQTT-3.1.3-6]
				clientId = Settings.SELF.getProperty("mqtt.defaultClientId", "lannisterDefaultClientId");
				cleanSession = true; // [MQTT-3.1.3-7]
			}
			else {
				returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
			}
		}
		else if (serviceStatus.isServiceAvailable() == false) {
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
		}
		else if (auth.isValid(msg.payload().clientIdentifier()) == false) {
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED; // [MQTT-3.1.3-9]
		}
		else if (auth.isValid(msg.variableHeader().hasUserName(), msg.variableHeader().hasPassword(),
				msg.payload().userName(), msg.payload().password()) == false) {
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;
		}
		else if (auth.isAuthorized(msg.variableHeader().hasUserName(), msg.payload().userName()) == false) {
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
		}

		if (returnCode != MqttConnectReturnCode.CONNECTION_ACCEPTED) {
			sendNoneAcceptMessage(ctx, returnCode, false); // [MQTT-3.2.2-4]
			return;
		}

		// TODO [MQTT-3.1.2-3] handling Reserved Flag, but netty variable header
		// doesn't have it

		session = LiveSessions.SELF.getByClientId(clientId);
		if (session != null) {
			LiveSessions.SELF.dispose(session); // [MQTT-3.1.4-2]
		}

		boolean sessionPresent = !cleanSession;
		if (cleanSession) {
			Repository.SELF.sessions().remove(clientId);

			session = new Session(ctx, clientId, msg.variableHeader().keepAliveTimeSeconds(), false); // [MQTT-3.1.2-6]

			LiveSessions.SELF.put(session);

			sessionPresent = false; // [MQTT-3.2.2-1]
		}
		else {
			session = Repository.SELF.sessions().get(clientId);

			if (session == null) {
				session = new Session(ctx, clientId, msg.variableHeader().keepAliveTimeSeconds(), true);
				sessionPresent = false; // [MQTT-3.2.2-3]
			}
			else {
				session.revive(ctx);
				sessionPresent = true; // [MQTT-3.2.2-2]
			}

			LiveSessions.SELF.put(session);
		}

		if (msg.variableHeader().isWillFlag()) {
			session.setWill(new Will(msg.payload().willTopic(), msg.payload().willMessage(),
					msg.variableHeader().willQos() == 0 ? MqttQoS.AT_MOST_ONCE : MqttQoS.AT_LEAST_ONCE,
					msg.variableHeader().isWillRetain()));
		}

		MqttConnAckMessage acceptMsg = MessageFactory.connAck(returnCode, sessionPresent);

		session.send(acceptMsg).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				eventListener.connAckMessageSent(acceptMsg);
			}
		});
	}

	private ChannelFuture sendNoneAcceptMessage(ChannelHandlerContext ctx, MqttConnectReturnCode returnCode,
			boolean sessionPresent) {
		MqttConnAckMessage msg = MessageFactory.connAck(returnCode, sessionPresent);

		ChannelFuture ret = ctx.channel().writeAndFlush(MessageFactory.connAck(returnCode, sessionPresent))
				.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						logger.debug("Message sent : {}", msg.toString());
						eventListener.connAckMessageSent(msg);
						LiveSessions.SELF.dispose(ctx); // [MQTT-3.2.2-5]
					}
				});

		return ret;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getMessage(), cause);

		MqttConnectReturnCode returnCode;

		if (MqttIdentifierRejectedException.class.getName().equals(cause.getClass().getName())) {
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
		}
		else if (IllegalArgumentException.class.getName().equals(cause.getClass().getName())
				&& cause.getMessage().contains(" is unknown mqtt version")) {
			// [MQTT-3.1.2-2]
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
		}
		else if (MqttUnacceptableProtocolVersionException.class.getName().equals(cause.getClass().getName())) {
			// [MQTT-3.1.2-2]
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
		}
		else {
			super.exceptionCaught(ctx, cause);
			return;
		}

		sendNoneAcceptMessage(ctx, returnCode, false); // [MQTT-3.2.2-4]
	}
}
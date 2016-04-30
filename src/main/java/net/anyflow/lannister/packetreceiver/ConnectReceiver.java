package net.anyflow.lannister.packetreceiver;

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
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.MessageFactory;
import net.anyflow.lannister.plugin.Authorization;
import net.anyflow.lannister.plugin.EventListener;
import net.anyflow.lannister.plugin.PluginFactory;
import net.anyflow.lannister.plugin.ServiceStatus;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;

public class ConnectReceiver extends SimpleChannelInboundHandler<MqttConnectMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConnectReceiver.class);

	EventListener eventListener = (EventListener) (new PluginFactory()).create(EventListener.class);
	ServiceStatus serviceStatus = (ServiceStatus) (new PluginFactory()).create(ServiceStatus.class);
	Authorization auth = (Authorization) (new PluginFactory()).create(Authorization.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttConnectMessage msg) throws Exception {
		logger.debug("packet incoming : {}", msg.toString());

		eventListener.connectMessageReceived(msg);

		Session session = Session.NEXUS.get(ctx.channel().id());
		if (session != null) {
			session.dispose(true); // [MQTT-3.1.0-2]
			return;
		}

		MqttConnectReturnCode returnCode = MqttConnectReturnCode.CONNECTION_ACCEPTED;

		boolean cleanSession = msg.variableHeader().isCleanSession();

		String clientId = msg.payload().clientIdentifier();

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

		final String clientIdFinal = clientId;
		session = Session.NEXUS.map().values().parallelStream()
				.filter(s -> s.isConnected() && clientIdFinal.equals(s.clientId())).findFirst().orElse(null);

		if (session != null && session.isConnected()) {
			session.dispose(false); // [MQTT-3.1.4-2]
		}

		boolean sessionPresent = !cleanSession;
		if (cleanSession) {
			session = new Session(clientId, msg.variableHeader().keepAliveTimeSeconds(), true, will(clientId, msg)); // [MQTT-3.1.2-6]

			Session.NEXUS.put(session, ctx);

			sessionPresent = false; // [MQTT-3.2.2-1]
		}
		else {
			session = Session.NEXUS.get(clientId);

			if (session == null) {
				session = new Session(clientId, msg.variableHeader().keepAliveTimeSeconds(), false,
						will(clientId, msg));
				sessionPresent = false; // [MQTT-3.2.2-3]
			}
			else {
				sessionPresent = true; // [MQTT-3.2.2-2]
			}

			Session.NEXUS.put(session, ctx);
		}

		if (session.will() != null) {
			Topic topic = Topic.NEXUS.get(session.will().topicName());
			if (topic == null) {
				topic = new Topic(session.will().topicName());
				Topic.put(topic);
			}

			if (session.will().isRetain()) { // [MQTT-3.1.2-16],[MQTT-3.1.2-17]
				topic.setRetainedMessage(session.will().message().length > 0 ? session.will() : null);
			}
		}

		final Session sessionFinal = session;
		final MqttConnAckMessage acceptMsg = MessageFactory.connack(returnCode, sessionPresent);

		session.send(acceptMsg).addListener(f -> {
			eventListener.connAckMessageSent(acceptMsg);

			if (sessionFinal.isCleanSession() == false) {
				sessionFinal.completeRemainedMessages();
			}
		});
	}

	private Message will(String clientId, MqttConnectMessage conn) {
		if (conn.variableHeader().isWillFlag() == false) { return null; } // [MQTT-3.1.2-12]

		return new Message(-1, conn.payload().willTopic(), clientId, conn.payload().willMessage().getBytes(),
				MqttQoS.valueOf(conn.variableHeader().willQos()), conn.variableHeader().isWillRetain());
	}

	private ChannelFuture sendNoneAcceptMessage(ChannelHandlerContext ctx, MqttConnectReturnCode returnCode,
			boolean sessionPresent) {
		MqttConnAckMessage msg = MessageFactory.connack(returnCode, sessionPresent);

		ChannelFuture ret = ctx.channel().writeAndFlush(MessageFactory.connack(returnCode, sessionPresent))
				.addListener(f -> {
					logger.debug("packet outgoing : {}", msg);

					eventListener.connAckMessageSent(msg);

					Session session = Session.NEXUS.get(ctx.channel().id());

					if (session != null) {
						session.dispose(true); // [MQTT-3.2.2-5]
					}
					else {
						ctx.channel().disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-3.2.2-5]
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
				&& cause.getMessage().contains("invalid QoS")) {
			// [MQTT-3.1.2-2]
			returnCode = MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
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
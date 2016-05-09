package net.anyflow.lannister.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import net.anyflow.lannister.message.MessageFactory;

public class MqttPacketReceiver extends SimpleChannelInboundHandler<MqttMessage> {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttPacketReceiver.class);

	private final MqttClient client;
	private final MessageReceiver receiver;
	private final SharedObject sharedObject;

	protected MqttPacketReceiver(MqttClient client, MessageReceiver receiver, SharedObject sharedObject) {
		this.client = client;
		this.receiver = receiver;
		this.sharedObject = sharedObject;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
		switch (msg.fixedHeader().messageType()) {
		case PUBLISH:
			if (receiver != null) {
				receiver.messageReceived(msg);
			}
			break;

		case CONNACK:
			sharedObject.receivedMessage(msg);

			synchronized (sharedObject.locker()) {
				sharedObject.locker().notify();
			}
			break;

		case PUBREC:
			client.send(MessageFactory.pubrel(((MqttMessageIdVariableHeader) msg.variableHeader()).messageId()));
			break;

		case SUBACK:
		case PUBACK:
		case PUBCOMP:
		default:
			break;
		}
	}
}
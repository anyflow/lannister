package net.anyflow.lannister.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;

public class MqttPacketReceiver extends SimpleChannelInboundHandler<MqttMessage> {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttPacketReceiver.class);

	private final MessageReceiver receiver;
	private final SharedObject sharedObject;

	protected MqttPacketReceiver(MessageReceiver receiver, SharedObject sharedObject) {
		this.receiver = receiver;
		this.sharedObject = sharedObject;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
		if (receiver == null) { return; }

		switch (msg.fixedHeader().messageType()) {
		case PUBLISH:
			receiver.messageReceived(msg);
			return;

		case CONNACK:
			sharedObject.receivedMessage(msg);

			synchronized (sharedObject.locker()) {
				sharedObject.locker().notify();
			}
			break;

		default:
			break;
		}
	}
}
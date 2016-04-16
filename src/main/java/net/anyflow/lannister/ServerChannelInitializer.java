package net.anyflow.lannister;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import net.anyflow.lannister.messagehandler.MqttConnectMessageHandler;
import net.anyflow.lannister.messagehandler.MqttPubAckMessageHandler;
import net.anyflow.lannister.messagehandler.MqttPublishMessageHandler;
import net.anyflow.lannister.messagehandler.MqttSubscribeMessageHandler;
import net.anyflow.lannister.messagehandler.MqttUnsubscribeMessageHandler;

public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ServerChannelInitializer.class);

	public ServerChannelInitializer() {

	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		if ("true".equalsIgnoreCase(Settings.SELF.getProperty("menton.logging.writelogOfNettyLogger"))) {
			ch.pipeline().addLast("log", new LoggingHandler(LogLevel.DEBUG));
		}

		ch.pipeline().addLast(MqttDecoder.class.getName(), new MqttDecoder());
		ch.pipeline().addLast(MqttEncoder.class.getName(), MqttEncoder.INSTANCE);

		ch.pipeline().addLast(MqttConnectMessageHandler.class.getName(), new MqttConnectMessageHandler());
		ch.pipeline().addLast(MqttPubAckMessageHandler.class.getName(), new MqttPubAckMessageHandler());
		ch.pipeline().addLast(MqttPublishMessageHandler.class.getName(), new MqttPublishMessageHandler());
		ch.pipeline().addLast(MqttSubscribeMessageHandler.class.getName(), new MqttSubscribeMessageHandler());
		ch.pipeline().addLast(MqttUnsubscribeMessageHandler.class.getName(), new MqttUnsubscribeMessageHandler());
	}
}
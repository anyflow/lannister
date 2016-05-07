package net.anyflow.lannister;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import net.anyflow.lannister.packetreceiver.ConnectReceiver;
import net.anyflow.lannister.packetreceiver.GenericReceiver;
import net.anyflow.lannister.packetreceiver.PubAckReceiver;
import net.anyflow.lannister.packetreceiver.PublishReceiver;
import net.anyflow.lannister.packetreceiver.SubscribeReceiver;
import net.anyflow.lannister.packetreceiver.UnsubscribeReceiver;

public class MqttChannelInitializer extends ChannelInitializer<SocketChannel> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttChannelInitializer.class);

	private final boolean useSsl;

	public MqttChannelInitializer(boolean useSsl) {
		this.useSsl = useSsl;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		logger.debug("Initializaing channels...");

		if ("true".equalsIgnoreCase(Settings.SELF.getProperty("netty.logger"))) {
			ch.pipeline().addLast(LoggingHandler.class.getName(), new LoggingHandler(LogLevel.DEBUG));
		}

		if (useSsl) {
			SslContext sslCtx = SslContextBuilder
					.forServer(Settings.SELF.certChainFile(), Settings.SELF.privateKeyFile()).build();

			logger.debug("SSL Provider : {}", SslContext.defaultServerProvider());

			ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
		}

		int maxBytesInMessage = Settings.SELF.getInt("lannister.maxBytesInMessage", 8092);

		ch.pipeline().addLast(MqttDecoder.class.getName(), new MqttDecoder(maxBytesInMessage));
		ch.pipeline().addLast(MqttEncoder.class.getName(), MqttEncoder.INSTANCE);

		ch.pipeline().addLast(ConnectReceiver.class.getName(), new ConnectReceiver());
		ch.pipeline().addLast(PubAckReceiver.class.getName(), new PubAckReceiver());
		ch.pipeline().addLast(PublishReceiver.class.getName(), new PublishReceiver());
		ch.pipeline().addLast(SubscribeReceiver.class.getName(), new SubscribeReceiver());
		ch.pipeline().addLast(UnsubscribeReceiver.class.getName(), new UnsubscribeReceiver());
		ch.pipeline().addLast(GenericReceiver.class.getName(), new GenericReceiver());
	}
}
package net.anyflow.lannister.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.packetreceiver.ConnectReceiver;
import net.anyflow.lannister.packetreceiver.GenericReceiver;
import net.anyflow.lannister.packetreceiver.PubAckReceiver;
import net.anyflow.lannister.packetreceiver.PublishReceiver;
import net.anyflow.lannister.packetreceiver.SubscribeReceiver;
import net.anyflow.lannister.packetreceiver.UnsubscribeReceiver;

public class MqttChannelInitializer extends ChannelInitializer<SocketChannel> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttChannelInitializer.class);

	private final boolean useWebSocket;
	private final boolean useSsl;

	public MqttChannelInitializer(boolean useWebSocket, boolean useSsl) {
		this.useWebSocket = useWebSocket;
		this.useSsl = useSsl;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		logger.debug("Initializaing channels...");

		ch.pipeline().addLast(ByteCounterCodec.class.getName(), new ByteCounterCodec());

		if ("true".equalsIgnoreCase(Settings.INSTANCE.getProperty("netty.logger"))) {
			ch.pipeline().addLast(LoggingHandler.class.getName(), new LoggingHandler(LogLevel.DEBUG));
		}

		if (useSsl) {
			SslContext sslCtx = SslContextBuilder
					.forServer(Settings.INSTANCE.certChainFile(), Settings.INSTANCE.privateKeyFile()).build();

			logger.debug("SSL Provider : {}", SslContext.defaultServerProvider());

			ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
		}

		if (useWebSocket) {
			String websocketPath = Settings.INSTANCE.getProperty("mqttserver.websocket.path", "/");

			ch.pipeline().addLast(HttpServerCodec.class.getName(), new HttpServerCodec());
			ch.pipeline().addLast(HttpObjectAggregator.class.getName(), new HttpObjectAggregator(1048576));
			ch.pipeline().addLast(HttpContentCompressor.class.getName(), new HttpContentCompressor());
			ch.pipeline().addLast(WebSocketServerProtocolHandler.class.getName(),
					new WebSocketServerProtocolHandler(websocketPath, "mqtt,mqttv3.1,mqttv3.1.1", true, 65536));
			ch.pipeline().addLast(new MqttWebSocketCodec());
		}

		int maxBytesInMessage = Settings.INSTANCE.getInt("mqttserver.maxBytesInMessage", 8092);

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
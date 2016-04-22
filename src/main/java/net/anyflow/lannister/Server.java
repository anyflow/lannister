package net.anyflow.lannister;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.anyflow.lannister.messagehandler.GenericMqttMessageHandler;
import net.anyflow.lannister.messagehandler.MqttConnectMessageHandler;
import net.anyflow.lannister.messagehandler.MqttPubAckMessageHandler;
import net.anyflow.lannister.messagehandler.MqttPublishMessageHandler;
import net.anyflow.lannister.messagehandler.MqttSubscribeMessageHandler;
import net.anyflow.lannister.messagehandler.MqttUnsubscribeMessageHandler;

public class Server {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Server.class);

	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;

	private static final int PORT = Settings.SELF.getInt("lannister.port", 1883);

	public Server() {
		bossGroup = new NioEventLoopGroup(Settings.SELF.getInt("lannister.system.bossThreadCount", 0),
				new DefaultThreadFactory("server/boss"));
		workerGroup = new NioEventLoopGroup(Settings.SELF.getInt("lannister.system.workerThreadCount", 0),
				new DefaultThreadFactory("server/worker"));
	}

	public void Start() {
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();

			bootstrap = bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);

			bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					logger.debug("Initializaing channels...");

					if ("true".equalsIgnoreCase(Settings.SELF.getProperty("netty.logger"))) {
						ch.pipeline().addLast(LoggingHandler.class.getName(), new LoggingHandler(LogLevel.DEBUG));
					}

					ch.pipeline().addLast(MqttDecoder.class.getName(), new MqttDecoder());
					ch.pipeline().addLast(MqttEncoder.class.getName(), MqttEncoder.INSTANCE);

					ch.pipeline().addLast(MqttConnectMessageHandler.class.getName(), new MqttConnectMessageHandler());
					ch.pipeline().addLast(MqttPubAckMessageHandler.class.getName(), new MqttPubAckMessageHandler());
					ch.pipeline().addLast(MqttPublishMessageHandler.class.getName(), new MqttPublishMessageHandler());
					ch.pipeline().addLast(MqttSubscribeMessageHandler.class.getName(),
							new MqttSubscribeMessageHandler());
					ch.pipeline().addLast(MqttUnsubscribeMessageHandler.class.getName(),
							new MqttUnsubscribeMessageHandler());
					ch.pipeline().addLast(GenericMqttMessageHandler.class.getName(), new GenericMqttMessageHandler());
				}
			});

			bootstrap.bind(PORT).sync();

			logger.info("Lannister server started: Port:{}", PORT);
		}
		catch (Exception e) {
			logger.error("Lannister failed to start...", e);
			shutdown();
		}
	}

	public void shutdown() {
		if (bossGroup != null) {
			bossGroup.shutdownGracefully().awaitUninterruptibly();
			logger.info("Boss event loop group shutdowned.");
		}

		if (workerGroup != null) {
			workerGroup.shutdownGracefully().awaitUninterruptibly();
			logger.info("Worker event loop group shutdowned.");
		}

		logger.info("Lannister server stopped.");
	}
}
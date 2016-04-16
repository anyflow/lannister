package net.anyflow.lannister;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

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

			bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new ServerChannelInitializer());

			bootstrap.bind(PORT).sync();

			logger.info("Lannister server started.");
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

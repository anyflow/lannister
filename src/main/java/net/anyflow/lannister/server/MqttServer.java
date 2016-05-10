/*
 * Copyright 2016 The Lannister Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.anyflow.lannister.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.packetreceiver.SessionExpirator;

public class MqttServer {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttServer.class);

	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;

	private static final Integer TCP_PORT = Settings.SELF.getInt("lannister.tcp.port", null);
	private static final Integer TCP_SSL_PORT = Settings.SELF.getInt("lannister.tcp.ssl.port", null);
	private static final Integer WEBSOCKET_PORT = Settings.SELF.getInt("lannister.websocket.port", null);
	private static final Integer WEBSOCKET_SSL_PORT = Settings.SELF.getInt("lannister.websocket.ssl.port", null);

	public MqttServer() {
		bossGroup = new NioEventLoopGroup(Settings.SELF.getInt("lannister.system.bossThreadCount", 0),
				new DefaultThreadFactory("lannister/boss"));
		workerGroup = new NioEventLoopGroup(Settings.SELF.getInt("lannister.system.workerThreadCount", 0),
				new DefaultThreadFactory("lannister/worker"));
	}

	public void start() throws Exception {
		if (TCP_PORT == null && TCP_SSL_PORT == null) {
			logger.info("No MQTT port(s) arranged");
			shutdown();
			return;
		}

		try {
			if (TCP_PORT != null) {
				ServerBootstrap bootstrap = new ServerBootstrap();

				bootstrap = bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);

				bootstrap.handler(new SessionExpirator()).childHandler(new MqttChannelInitializer(false, false));
				bootstrap.bind(TCP_PORT).sync();
			}
			if (TCP_SSL_PORT != null) {
				ServerBootstrap bootstrap = new ServerBootstrap();

				bootstrap = bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);

				bootstrap.handler(new SessionExpirator()).childHandler(new MqttChannelInitializer(false, true));
				bootstrap.bind(TCP_SSL_PORT).sync();
			}
			if (WEBSOCKET_PORT != null) {
				ServerBootstrap bootstrap = new ServerBootstrap();

				bootstrap = bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);

				bootstrap.handler(new SessionExpirator()).childHandler(new MqttChannelInitializer(true, false));
				bootstrap.bind(WEBSOCKET_PORT).sync();
			}
			if (WEBSOCKET_SSL_PORT != null) {
				ServerBootstrap bootstrap = new ServerBootstrap();

				bootstrap = bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);

				bootstrap.handler(new SessionExpirator()).childHandler(new MqttChannelInitializer(true, true));
				bootstrap.bind(WEBSOCKET_SSL_PORT).sync();
			}

			logger.info(
					"Lannister server started: [MQTT tcp.port={}, tcp.ssl.port={}, websocket.port={}, websocket.ssl.port={}]",
					TCP_PORT, TCP_SSL_PORT, WEBSOCKET_PORT, WEBSOCKET_SSL_PORT);
		}
		catch (Exception e) {
			logger.error("Lannister failed to start", e);

			shutdown();

			throw e;
		}
	}

	public void shutdown() {
		if (bossGroup != null) {
			bossGroup.shutdownGracefully().awaitUninterruptibly();
			logger.info("Boss event loop group shutdowned");
		}

		if (workerGroup != null) {
			workerGroup.shutdownGracefully().awaitUninterruptibly();
			logger.info("Worker event loop group shutdowned");
		}

		logger.info("Lannister server shutdowned");
	}
}
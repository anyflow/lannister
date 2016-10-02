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

import java.util.concurrent.ThreadFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.anyflow.lannister.Literals;
import net.anyflow.lannister.Settings;

public class MqttServer {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttServer.class);

	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;

	public MqttServer() {
		int bossThreadCount = Settings.INSTANCE.getInt("mqttserver.system.bossThreadCount", 0);
		int workerThreadCount = Settings.INSTANCE.getInt("mqttserver.system.workerThreadCount", 0);

		ThreadFactory bossThreadFactory = new DefaultThreadFactory("lannister/boss");
		ThreadFactory workerThreadFactory = new DefaultThreadFactory("lannister/worker");

		if (Literals.NETTY_EPOLL.equals(Settings.INSTANCE.nettyTransportMode())) {
			bossGroup = new EpollEventLoopGroup(bossThreadCount, bossThreadFactory);
			workerGroup = new EpollEventLoopGroup(workerThreadCount, workerThreadFactory);
		}
		else {
			bossGroup = new NioEventLoopGroup(bossThreadCount, bossThreadFactory);
			workerGroup = new NioEventLoopGroup(workerThreadCount, workerThreadFactory);
		}
	}

	public void start() throws Exception {
		if (Settings.INSTANCE.mqttPort() == null && Settings.INSTANCE.mqttsPort() == null
				&& Settings.INSTANCE.websocketPort() == null && Settings.INSTANCE.websocketSslPort() == null) {
			logger.info("No MQTT port(s) arranged");
			shutdown();
			return;
		}

		try {
			ScheduledExecutor scheduledExecutor = new ScheduledExecutor();

			if (Settings.INSTANCE.mqttPort() != null) {
				executeBootstrap(scheduledExecutor, Settings.INSTANCE.mqttPort(), false, false);
				scheduledExecutor = null;
			}
			if (Settings.INSTANCE.mqttsPort() != null) {
				executeBootstrap(scheduledExecutor, Settings.INSTANCE.mqttsPort(), false, true);
				scheduledExecutor = null;
			}
			if (Settings.INSTANCE.websocketPort() != null) {
				executeBootstrap(scheduledExecutor, Settings.INSTANCE.websocketPort(), true, false);
				scheduledExecutor = null;
			}
			if (Settings.INSTANCE.websocketSslPort() != null) {
				executeBootstrap(scheduledExecutor, Settings.INSTANCE.websocketSslPort(), true, true);
				scheduledExecutor = null;
			}

			logger.info(
					"Lannister MQTT server started [tcp.port={}, tcp.ssl.port={}, websocket.port={}, websocket.ssl.port={}]",
					Settings.INSTANCE.mqttPort(), Settings.INSTANCE.mqttsPort(), Settings.INSTANCE.websocketPort(),
					Settings.INSTANCE.websocketSslPort());
		}
		catch (Exception e) {
			logger.error("Lannister MQTT server failed to start", e);

			shutdown();

			throw e;
		}
	}

	private void executeBootstrap(ScheduledExecutor scheduledExecutor, int port, boolean useWebSocket, boolean useSsl)
			throws InterruptedException {
		ServerBootstrap bootstrap = new ServerBootstrap();

		Class<? extends ServerChannel> serverChannelClass;

		if (Literals.NETTY_EPOLL.equals(Settings.INSTANCE.nettyTransportMode())) {
			serverChannelClass = EpollServerSocketChannel.class;
		}
		else {
			serverChannelClass = NioServerSocketChannel.class;
		}

		bootstrap = bootstrap.group(bossGroup, workerGroup).channel(serverChannelClass);

		if (scheduledExecutor != null) {
			bootstrap.handler(scheduledExecutor);
		}

		bootstrap.childHandler(new MqttChannelInitializer(useWebSocket, useSsl));
		bootstrap.bind(port).sync();
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

		logger.info("Lannister MQTT server stopped");
	}
}
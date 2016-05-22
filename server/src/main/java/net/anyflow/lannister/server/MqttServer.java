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

public class MqttServer {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttServer.class);

	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;

	public MqttServer() {
		bossGroup = new NioEventLoopGroup(Settings.SELF.getInt("lannister.system.bossThreadCount", 0),
				new DefaultThreadFactory("lannister/boss"));
		workerGroup = new NioEventLoopGroup(Settings.SELF.getInt("lannister.system.workerThreadCount", 0),
				new DefaultThreadFactory("lannister/worker"));
	}

	public void start() throws Exception {
		if (Settings.SELF.mqttPort() == null && Settings.SELF.mqttsPort() == null
				&& Settings.SELF.websocketPort() == null && Settings.SELF.websocketSslPort() == null) {
			logger.info("No MQTT port(s) arranged");
			shutdown();
			return;
		}

		try {
			ScheduledExecutor scheduledExecutor = new ScheduledExecutor();

			if (Settings.SELF.mqttPort() != null) {
				executeBootstrap(scheduledExecutor, Settings.SELF.mqttPort(), false, false);
				scheduledExecutor = null;
			}
			if (Settings.SELF.mqttsPort() != null) {
				executeBootstrap(scheduledExecutor, Settings.SELF.mqttsPort(), false, true);
				scheduledExecutor = null;
			}
			if (Settings.SELF.websocketPort() != null) {
				executeBootstrap(scheduledExecutor, Settings.SELF.websocketPort(), true, false);
				scheduledExecutor = null;
			}
			if (Settings.SELF.websocketSslPort() != null) {
				executeBootstrap(scheduledExecutor, Settings.SELF.websocketSslPort(), true, true);
				scheduledExecutor = null;
			}

			logger.info(
					"Lannister MQTT server started [tcp.port={}, tcp.ssl.port={}, websocket.port={}, websocket.ssl.port={}]",
					Settings.SELF.mqttPort(), Settings.SELF.mqttsPort(), Settings.SELF.websocketPort(),
					Settings.SELF.websocketSslPort());
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

		bootstrap = bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);

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
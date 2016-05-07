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
import net.anyflow.lannister.packetreceiver.ConnectReceiver;
import net.anyflow.lannister.packetreceiver.GenericReceiver;
import net.anyflow.lannister.packetreceiver.PubAckReceiver;
import net.anyflow.lannister.packetreceiver.PublishReceiver;
import net.anyflow.lannister.packetreceiver.SessionExpirator;
import net.anyflow.lannister.packetreceiver.SubscribeReceiver;
import net.anyflow.lannister.packetreceiver.UnsubscribeReceiver;

public class MqttServer {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MqttServer.class);

	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;

	private static final int PORT = Settings.SELF.getInt("lannister.port", 1883);

	public MqttServer() {
		bossGroup = new NioEventLoopGroup(Settings.SELF.getInt("lannister.system.bossThreadCount", 0),
				new DefaultThreadFactory("lannister/boss"));
		workerGroup = new NioEventLoopGroup(Settings.SELF.getInt("lannister.system.workerThreadCount", 0),
				new DefaultThreadFactory("lannister/worker"));
	}

	public void Start() throws Exception {
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();

			bootstrap = bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class);

			bootstrap.handler(new SessionExpirator()).childHandler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					logger.debug("Initializaing channels...");

					if ("true".equalsIgnoreCase(Settings.SELF.getProperty("netty.logger"))) {
						ch.pipeline().addLast(LoggingHandler.class.getName(), new LoggingHandler(LogLevel.DEBUG));
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
			});

			bootstrap.bind(PORT).sync();

			logger.info("Lannister server started: [MQTT port={}]", PORT);
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
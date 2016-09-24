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

package net.anyflow.lannister.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.anyflow.lannister.Settings;

public class WebServer {

	private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;

	public WebServer() {
		bossGroup = new NioEventLoopGroup(Settings.INSTANCE.getInt("webserver.system.bossThreadCount", 0),
				new DefaultThreadFactory("lannister.web/boss"));
		workerGroup = new NioEventLoopGroup(Settings.INSTANCE.getInt("webserver.system.workerThreadCount", 0),
				new DefaultThreadFactory("lannister.web/worker"));
	}

	public EventLoopGroup bossGroup() {
		return bossGroup;
	}

	public EventLoopGroup workerGroup() {
		return workerGroup;
	}

	public void start(String requestHandlerPakcageRoot) throws Exception {
		start(requestHandlerPakcageRoot, null);
	}

	public void start(String requestHandlerPakcageRoot,
			final Class<? extends WebsocketFrameHandler> websocketFrameHandlerClass) throws Exception {
		HttpRequestHandler.setRequestHandlerPakcageRoot(requestHandlerPakcageRoot);
		try {
			if (Settings.INSTANCE.httpPort() != null) {
				ServerBootstrap bootstrap = new ServerBootstrap();

				bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
						.childHandler(new WebServerChannelInitializer(false, websocketFrameHandlerClass));

				bootstrap.bind(Settings.INSTANCE.httpPort()).sync();
			}

			if (Settings.INSTANCE.httpsPort() != null) {
				ServerBootstrap bootstrap = new ServerBootstrap();

				bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
						.childHandler(new WebServerChannelInitializer(true, websocketFrameHandlerClass));

				bootstrap.bind(Settings.INSTANCE.httpsPort()).sync();
			}

			logger.info("Lannister HTTP server started [http.port={}, https.port={}]", Settings.INSTANCE.httpPort(),
					Settings.INSTANCE.httpsPort());
		}
		catch (Exception e) {
			logger.error("Lannister HTTP server failed to start...", e);

			shutdown();

			throw e;
		}
	}

	public void shutdown() {
		if (bossGroup != null) {
			bossGroup.shutdownGracefully().awaitUninterruptibly();
			logger.debug("Boss event loop group shutdowned");
		}

		if (workerGroup != null) {
			workerGroup.shutdownGracefully().awaitUninterruptibly();
			logger.debug("Worker event loop group shutdowned");
		}

		logger.debug("Lannister HTTP server stopped");
	}
}
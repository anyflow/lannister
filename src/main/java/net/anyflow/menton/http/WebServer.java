/*
 * Copyright 2016 The Menton Project
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

package net.anyflow.menton.http;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.anyflow.lannister.Settings;
import net.anyflow.menton.general.TaskCompletionInformer;
import net.anyflow.menton.general.TaskCompletionListener;

/**
 * @author anyflow
 */
public class WebServer implements TaskCompletionInformer {

	private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;
	private final List<TaskCompletionListener> taskCompletionListeners;

	public WebServer() {
		taskCompletionListeners = Lists.newArrayList();

		bossGroup = new NioEventLoopGroup(Settings.SELF.getInt("menton.system.bossThreadCount", 0),
				new DefaultThreadFactory("menton/boss"));
		workerGroup = new NioEventLoopGroup(Settings.SELF.getInt("menton.system.workerThreadCount", 0),
				new DefaultThreadFactory("menton/worker"));
	}

	public EventLoopGroup bossGroup() {
		return bossGroup;
	}

	public EventLoopGroup workerGroup() {
		return workerGroup;
	}

	/**
	 * @param requestHandlerPakcageRoot
	 *            root package prefix of request handlers.
	 * @return the HTTP channel
	 * @throws Exception
	 */
	public void start(String requestHandlerPakcageRoot) throws Exception {
		start(requestHandlerPakcageRoot, null);
	}

	/**
	 * @param requestHandlerPakcageRoot
	 *            root package prefix of request handlers.
	 * @param webSocketFrameHandler
	 *            websocket handler
	 * @return the HTTP channel
	 * @throws Exception
	 */
	public void start(String requestHandlerPakcageRoot,
			final Class<? extends WebsocketFrameHandler> websocketFrameHandlerClass) throws Exception {
		HttpRequestHandler.setRequestHandlerPakcageRoot(requestHandlerPakcageRoot);
		try {
			if (Settings.SELF.httpPort() != null) {
				ServerBootstrap bootstrap = new ServerBootstrap();

				bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
						.childHandler(new WebServerChannelInitializer(false, websocketFrameHandlerClass));

				bootstrap.bind(Settings.SELF.httpPort()).sync();
			}

			if (Settings.SELF.httpsPort() != null) {
				ServerBootstrap bootstrap = new ServerBootstrap();

				bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
						.childHandler(new WebServerChannelInitializer(true, websocketFrameHandlerClass));

				bootstrap.bind(Settings.SELF.httpsPort()).sync();
			}

			logger.info("Menton server started: [HTTP port={}, HTTPS port={}]", Settings.SELF.httpPort(),
					Settings.SELF.httpsPort());
		}
		catch (Exception e) {
			logger.error("Menton server failed to start...", e);

			shutdown();

			throw e;
		}
	}

	public void shutdown() {
		if (bossGroup != null) {
			bossGroup.shutdownGracefully().awaitUninterruptibly();
			logger.debug("Boss event loop group shutdowned.");
		}

		if (workerGroup != null) {
			workerGroup.shutdownGracefully().awaitUninterruptibly();
			logger.debug("Worker event loop group shutdowned.");
		}

		logger.debug("Menton server stopped");
		inform();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.anyflow.menton.general.TaskCompletionInformer#register(net.anyflow.
	 * menton.general.TaskCompletionListener)
	 */
	@Override
	public void register(TaskCompletionListener taskCompletionListener) {
		taskCompletionListeners.add(taskCompletionListener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.anyflow.menton.general.TaskCompletionInformer#deregister(net.anyflow.
	 * menton.general.TaskCompletionListener)
	 */
	@Override
	public void deregister(TaskCompletionListener taskCompletionListener) {
		taskCompletionListeners.remove(taskCompletionListener);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.general.TaskCompletionInformer#inform()
	 */
	@Override
	public void inform() {
		for (TaskCompletionListener taskCompletionListener : taskCompletionListeners) {
			taskCompletionListener.taskCompleted(this, false);
		}
	}
}
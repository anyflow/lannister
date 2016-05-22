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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import net.anyflow.lannister.Settings;

class WebServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WebServerChannelInitializer.class);

	private final boolean useSsl;
	private final Class<? extends WebsocketFrameHandler> websocketFrameHandlerClass;

	public WebServerChannelInitializer(boolean useSsl,
			Class<? extends WebsocketFrameHandler> websocketFrameHandlerClass) {
		this.useSsl = useSsl;
		this.websocketFrameHandlerClass = websocketFrameHandlerClass;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		if ("true".equalsIgnoreCase(Settings.SELF.getProperty("lannister.web.logging.writelogOfNettyLogger"))) {
			ch.pipeline().addLast("log", new LoggingHandler("lannister.web/server", LogLevel.DEBUG));
		}

		if (useSsl) {
			SslContext sslCtx = SslContextBuilder
					.forServer(Settings.SELF.certChainFile(), Settings.SELF.privateKeyFile()).build();

			logger.debug("SSL Provider : {}", SslContext.defaultServerProvider());

			ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
		}

		ch.pipeline().addLast(HttpServerCodec.class.getName(), new HttpServerCodec());
		ch.pipeline().addLast(HttpObjectAggregator.class.getName(), new HttpObjectAggregator(1048576));
		ch.pipeline().addLast(HttpContentCompressor.class.getName(), new HttpContentCompressor());
		ch.pipeline().addLast(HttpRequestRouter.class.getName(), new HttpRequestRouter());

		if (websocketFrameHandlerClass != null) {
			WebsocketFrameHandler wsfh = websocketFrameHandlerClass.newInstance();

			ch.pipeline().addLast(WebSocketServerProtocolHandler.class.getName(), new WebSocketServerProtocolHandler(
					wsfh.websocketPath(), wsfh.subprotocols(), wsfh.allowExtensions(), wsfh.maxFrameSize()));

			ch.pipeline().addLast(wsfh);
		}
	}
}
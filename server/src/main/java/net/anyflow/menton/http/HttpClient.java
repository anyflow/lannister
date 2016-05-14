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

import java.net.URISyntaxException;

import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.DefaultThreadFactory;
import net.anyflow.lannister.Settings;

/**
 * @author anyflow
 */
public class HttpClient implements IHttpClient {

	private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

	private final Bootstrap bootstrap;
	private final HttpRequest httpRequest;
	private final TrustManagerFactory trustManagerFactory;

	public HttpClient(String uri) throws UnsupportedOperationException, URISyntaxException {
		this(uri, false);
	}

	public HttpClient(String uri, boolean useInsecureTrustManagerFactory)
			throws URISyntaxException, UnsupportedOperationException {
		trustManagerFactory = useInsecureTrustManagerFactory ? InsecureTrustManagerFactory.INSTANCE : null;

		bootstrap = new Bootstrap();

		httpRequest = new HttpRequest(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri));

		if (httpRequest().uriObject().getScheme().equalsIgnoreCase("http") == false
				&& httpRequest().uriObject().getScheme().equalsIgnoreCase("https") == false) {
			String message = "HTTP(S) is supported only.";
			logger.error(message);
			throw new UnsupportedOperationException(message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.http.I#httpRequest()
	 */
	@Override
	public HttpRequest httpRequest() {
		return httpRequest;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.http.I#get()
	 */
	@Override
	public HttpResponse get() {
		return get(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.anyflow.menton.http.I#get(net.anyflow.menton.http.MessageReceiver)
	 */
	@Override
	public HttpResponse get(final MessageReceiver receiver) {
		httpRequest().setMethod(HttpMethod.GET);

		return request(receiver);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.http.I#post()
	 */
	@Override
	public HttpResponse post() {
		return post(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.anyflow.menton.http.I#post(net.anyflow.menton.http.MessageReceiver)
	 */
	@Override
	public HttpResponse post(final MessageReceiver receiver) {
		httpRequest().setMethod(HttpMethod.POST);

		return request(receiver);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.http.I#put()
	 */
	@Override
	public HttpResponse put() {
		return put(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.anyflow.menton.http.I#put(net.anyflow.menton.http.MessageReceiver)
	 */
	@Override
	public HttpResponse put(final MessageReceiver receiver) {
		httpRequest().setMethod(HttpMethod.PUT);

		return request(receiver);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.http.I#delete()
	 */
	@Override
	public HttpResponse delete() {
		return delete(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.anyflow.menton.http.I#delete(net.anyflow.menton.http.MessageReceiver)
	 */
	@Override
	public HttpResponse delete(final MessageReceiver receiver) {
		httpRequest().setMethod(HttpMethod.DELETE);

		return request(receiver);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.menton.http.I#setOption(io.netty.channel.ChannelOption,
	 * T)
	 */
	@Override
	public <T> IHttpClient setOption(ChannelOption<T> option, T value) {
		bootstrap.option(option, value);

		return this;
	}

	/**
	 * request.
	 * 
	 * @param receiver
	 * @return if receiver is not null the request processed successfully,
	 *         returns HttpResponse instance, otherwise null.
	 */
	private HttpResponse request(final MessageReceiver receiver) {
		httpRequest().normalize();
		setDefaultHeaders(httpRequest());

		if (logger.isDebugEnabled()) {
			logger.debug(httpRequest().toString());
		}

		final HttpClientHandler clientHandler = new HttpClientHandler(receiver, httpRequest);

		final EventLoopGroup group = new NioEventLoopGroup(1, new DefaultThreadFactory("client"));
		bootstrap.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {

				if ("true".equalsIgnoreCase(Settings.SELF.getProperty("menton.logging.writelogOfNettyLogger"))) {
					ch.pipeline().addLast("log", new LoggingHandler("menton/server", LogLevel.DEBUG));
				}

				if ("https".equalsIgnoreCase(httpRequest().uriObject().getScheme())) {
					SslContext sslCtx = SslContextBuilder.forClient().trustManager(trustManagerFactory).build();

					ch.pipeline().addLast(sslCtx.newHandler(ch.alloc(), httpRequest().uriObject().getHost(),
							httpRequest().uriObject().getPort()));
				}

				ch.pipeline().addLast("codec", new HttpClientCodec());
				ch.pipeline().addLast("inflater", new HttpContentDecompressor());
				ch.pipeline().addLast("chunkAggregator", new HttpObjectAggregator(1048576));
				ch.pipeline().addLast("handler", clientHandler);
			}
		});

		try {
			Channel channel = bootstrap
					.connect(httpRequest().uriObject().getHost(), httpRequest().uriObject().getPort()).sync().channel();
			channel.writeAndFlush(httpRequest);

			if (receiver == null) {
				channel.closeFuture().sync();
				group.shutdownGracefully();

				return clientHandler.httpResponse();
			}
			else {
				channel.closeFuture().addListener(new ChannelFutureListener() {

					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						group.shutdownGracefully();
					}
				});

				return null;
			}
		}
		catch (Exception e) {
			group.shutdownGracefully();
			logger.error(e.getMessage(), e);

			return null;
		}

	}

	protected static void setDefaultHeaders(HttpRequest httpRequest) {
		if (httpRequest.headers().contains(HttpHeaderNames.HOST) == false) {
			httpRequest.headers().set(HttpHeaderNames.HOST, httpRequest.uriObject().getHost());
		}
		if (httpRequest.headers().contains(HttpHeaderNames.CONNECTION) == false) {
			httpRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		}
		if (httpRequest.headers().contains(HttpHeaderNames.ACCEPT_ENCODING) == false) {
			httpRequest.headers().set(HttpHeaderNames.ACCEPT_ENCODING,
					HttpHeaderValues.GZIP + ", " + HttpHeaderValues.DEFLATE);
		}
		if (httpRequest.headers().contains(HttpHeaderNames.ACCEPT_CHARSET) == false) {
			httpRequest.headers().set(HttpHeaderNames.ACCEPT_CHARSET, "utf-8");
		}
		if (httpRequest.headers().contains(HttpHeaderNames.CONTENT_TYPE) == false) {
			httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
		}
	}
}
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import com.google.common.io.Files;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import net.anyflow.lannister.Application;
import net.anyflow.lannister.Settings;

public class HttpRequestRouter extends SimpleChannelInboundHandler<FullHttpRequest> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HttpRequestRouter.class);

	private boolean isStaticResourcePath(String path) {
		String ext = Files.getFileExtension(path);
		if (ext == null) { return false; }

		return Settings.INSTANCE.webResourceExtensionToMimes().keySet().stream().anyMatch(s -> s.equalsIgnoreCase(ext));
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest rawRequest) throws Exception {
		Date now = new Date();
		HttpRequest request = new HttpRequest(rawRequest);

		if (HttpHeaderValues.WEBSOCKET.toString().equalsIgnoreCase(rawRequest.headers().get(HttpHeaderNames.UPGRADE))
				&& HttpHeaderValues.UPGRADE.toString()
						.equalsIgnoreCase(rawRequest.headers().get(HttpHeaderNames.CONNECTION))) {

			if (ctx.pipeline().get(WebsocketFrameHandler.class) == null) {
				logger.error("No WebSocket Handler available");

				ctx.channel()
						.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN))
						.addListener(ChannelFutureListener.CLOSE);
				return;
			}

			ctx.fireChannelRead(rawRequest.retain());
			return;
		}

		if (HttpUtil.is100ContinueExpected(rawRequest)) {
			ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
			return;
		}

		HttpResponse response = HttpResponse.createServerDefault(request.headers().get(HttpHeaderNames.COOKIE));

		String requestPath = new URI(request.uri()).getPath();

		if (isStaticResourcePath(requestPath)) {
			handleStaticResource(response, requestPath);
		}
		else {
			try {
				handleDynamicResource(request, response);
			}
			catch (URISyntaxException e) {
				set404Response(response);
			}
			catch (Exception e) {
				response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
				logger.error("Unknown exception was thrown in business logic handler.\r\n{}", e.getMessage(), e);
			}
		}

		setDefaultHeaders(rawRequest, response);

		if ("true".equalsIgnoreCase(Settings.INSTANCE.getProperty("webserver.logging.writeHttpRequest"))) {
			logger.info(request.toString(now));
		}
		if ("true".equalsIgnoreCase(Settings.INSTANCE.getProperty("webserver.logging.writeHttpResponse"))) {
			logger.info(response.toString());
		}

		ctx.write(response);
	}

	private void set404Response(HttpResponse response) {
		response.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
		response.setStatus(HttpResponseStatus.NOT_FOUND);
		response.setContent(HttpResponseStatus.NOT_FOUND.toString());
	}

	private void handleStaticResource(HttpResponse response, String webResourceRequestPath) throws IOException {
		String requestPath = webResourceRequestPath;
		if (requestPath.startsWith("/")) {
			requestPath = requestPath.substring(1, requestPath.length());
		}

		InputStream is = Application.class.getClassLoader().getResourceAsStream(requestPath);

		try {
			if (is == null) {
				set404Response(response);
			}
			else {
				response.content().writeBytes(IOUtils.toByteArray(is));

				String ext = Files.getFileExtension(requestPath);
				response.headers().set(HttpHeaderNames.CONTENT_TYPE,
						Settings.INSTANCE.webResourceExtensionToMimes().get(ext));
			}
		}
		finally {
			if (is != null) {
				is.close();
			}
		}
	}

	private void handleDynamicResource(HttpRequest request, HttpResponse response)
			throws InstantiationException, IllegalAccessException, IOException, URISyntaxException {
		HttpRequestHandler.MatchedCriterion mc = HttpRequestHandler.findRequestHandler(new URI(request.uri()).getPath(),
				request.method().toString());

		if (mc.requestHandlerClass() == null) {
			set404Response(response);
		}
		else {
			HttpRequestHandler handler = mc.requestHandlerClass().newInstance();

			String webResourcePath = handler.getClass().getAnnotation(HttpRequestHandler.Handles.class)
					.webResourcePath();

			if (HttpRequestHandler.NO_WEB_RESOURCE_PATH.equals(webResourcePath)) {
				response.setContent(
						handler.initialize(request.pathParameters(mc.pathParameters()), response).service());
			}
			else {
				handleStaticResource(response, webResourcePath);
			}
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getMessage(), cause);
		ctx.close();
	}

	protected static void setDefaultHeaders(FullHttpRequest request, HttpResponse response) {
		response.headers().add(HttpHeaderNames.SERVER,
				"lannister " + net.anyflow.lannister.Settings.INSTANCE.version());

		boolean keepAlive = HttpHeaderValues.KEEP_ALIVE.toString()
				.equals(request.headers().get(HttpHeaderNames.CONNECTION));
		if (keepAlive) {
			response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		}

		if (Settings.INSTANCE.getProperty("webserver.allowCrossDomain", "false").equalsIgnoreCase("true")) {
			response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "POST, GET, PUT, DELETE");
			response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "X-PINGARUNER");
			response.headers().add(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "1728000");
		}

		response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
	}
}
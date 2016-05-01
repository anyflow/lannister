package net.anyflow.menton.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

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
import net.anyflow.lannister.Settings;

/**
 * @author anyflow
 */
public class HttpRequestRouter extends SimpleChannelInboundHandler<FullHttpRequest> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HttpRequestRouter.class);

	protected HttpRequestRouter() {
	}

	private boolean isWebResourcePath(String path) {
		return Settings.SELF.webResourceExtensionToMimes().keySet().stream().anyMatch(s -> path.endsWith("." + s));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.
	 * channel.ChannelHandlerContext, java.lang.Object)
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		if (HttpHeaderValues.WEBSOCKET.toString().equalsIgnoreCase(request.headers().get(HttpHeaderNames.UPGRADE))
				&& HttpHeaderValues.UPGRADE.toString()
						.equalsIgnoreCase(request.headers().get(HttpHeaderNames.CONNECTION))) {

			if (ctx.pipeline().get(WebsocketFrameHandler.class) == null) {
				logger.error("No WebSocket Handler available.");

				ctx.channel()
						.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN))
						.addListener(ChannelFutureListener.CLOSE);
				return;
			}

			ctx.fireChannelRead(request.retain());
			return;
		}

		if (HttpUtil.is100ContinueExpected(request)) {
			ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
			return;
		}

		HttpResponse response = HttpResponse.createServerDefault(request.headers().get(HttpHeaderNames.COOKIE));

		String requestPath = new URI(request.uri()).getPath();

		if (isWebResourcePath(requestPath)) {
			handleWebResourceRequest(ctx, request, response, requestPath);
		}
		else {
			try {
				processRequest(ctx, request, response);
			}
			catch (URISyntaxException e) {
				response.setStatus(HttpResponseStatus.NOT_FOUND);
				logger.info("unexcepted URI : {}", request.uri());
			}
			catch (Exception e) {
				response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
				logger.error("Unknown exception was thrown in business logic handler.\r\n" + e.getMessage(), e);
			}
		}
	}

	/**
	 * @param response
	 * @param webResourceRequestPath
	 * @throws IOException
	 */
	private void handleWebResourceRequest(ChannelHandlerContext ctx, FullHttpRequest rawRequest, HttpResponse response,
			String webResourceRequestPath) throws IOException {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(webResourceRequestPath);

		if (is == null) {
			String rootPath = (new File(Settings.SELF.webResourcePhysicalRootPath(), webResourceRequestPath)).getPath();
			try {
				is = new FileInputStream(rootPath);
			}
			catch (FileNotFoundException e) {
				is = null;
			}
		}

		if (is == null) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
		}
		else {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();
			response.content().writeBytes(buffer.toByteArray());

			String ext = Files.getFileExtension(webResourceRequestPath);
			response.headers().set(HttpHeaderNames.CONTENT_TYPE, Settings.SELF.webResourceExtensionToMimes().get(ext));

			is.close();
		}

		setDefaultHeaders(rawRequest, response);

		if ("true".equalsIgnoreCase(Settings.SELF.getProperty("menton.logging.writeHttpResponse"))) {
			logger.info(response.toString());
		}

		ctx.write(response);
	}

	private void setDefaultHeaders(FullHttpRequest request, HttpResponse response) {

		response.headers().add(HttpHeaderNames.SERVER,

				net.anyflow.lannister.Settings.SELF.getProperty("menton.version"));

		boolean keepAlive = request.headers().get(HttpHeaderNames.CONNECTION) == HttpHeaderValues.KEEP_ALIVE.toString();
		if (keepAlive) {
			response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		}

		if (Settings.SELF.getProperty("menton.httpServer.allowCrossDomain", "false").equalsIgnoreCase("true")) {
			response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "POST, GET, PUT, DELETE");
			response.headers().add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "X-PINGARUNER");
			response.headers().add(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "1728000");
		}

		response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
	}

	private void processRequest(ChannelHandlerContext ctx, FullHttpRequest rawRequest, HttpResponse response)
			throws InstantiationException, IllegalAccessException, IOException, URISyntaxException {

		HttpRequestHandler.MatchedCriterion mc = HttpRequestHandler
				.findRequestHandler((new URI(rawRequest.uri())).getPath(), rawRequest.method().toString());

		if (mc.requestHandlerClass() == null) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			logger.info("unexcepted URI : {}", rawRequest.uri());

			response.headers().add(HttpHeaderNames.CONTENT_TYPE, "text/html");

			response.setContent(response.toString());
		}
		else {
			HttpRequest request = new HttpRequest(rawRequest, mc.pathParameters());

			HttpRequestHandler handler = mc.requestHandlerClass().newInstance();

			String webResourcePath = handler.getClass().getAnnotation(HttpRequestHandler.Handles.class)
					.webResourcePath();
			if ("none".equals(webResourcePath) == false) {
				handleWebResourceRequest(ctx, rawRequest, response, webResourcePath);
				return;
			}

			handler.initialize(request, response);

			if ("true".equalsIgnoreCase(Settings.SELF.getProperty("menton.logging.writeHttpRequest"))) {
				logger.info(request.toString());
			}

			response.setContent(handler.service());
		}

		setDefaultHeaders(rawRequest, response);

		if ("true".equalsIgnoreCase(Settings.SELF.getProperty("menton.logging.writeHttpResponse"))) {
			logger.info(response.toString());
		}

		ctx.write(response);
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
}
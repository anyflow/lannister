package net.anyflow.menton.http;

import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.anyflow.lannister.Settings;

public class MockHttpServer {

	private static final Logger logger = LoggerFactory.getLogger(MockHttpServer.class);

	public MockHttpServer(String requestHandlerPakcageRoot) {
		HttpRequestHandler.setRequestHandlerPakcageRoot(requestHandlerPakcageRoot);
	}

	public HttpResponse service(HttpRequest httpRequest) {

		HttpResponse response = HttpResponse.createServerDefault(httpRequest.headers().get(HttpHeaderNames.COOKIE));

		HttpRequestHandler.MatchedCriterion mc = HttpRequestHandler
				.findRequestHandler(httpRequest.uriObject().getPath(), httpRequest.method().toString());

		if (mc.requestHandlerClass() == null) {
			response.setStatus(HttpResponseStatus.NOT_FOUND);
			logger.info("unexcepted URI : {}", httpRequest.uri());

			response.headers().add(HttpHeaderNames.CONTENT_TYPE, "text/html");

			response.setContent(response.toString());
		}
		else {
			HttpRequest request;
			try {
				request = new HttpRequest(httpRequest, mc.pathParameters());

				HttpRequestHandler handler;
				try {
					handler = mc.requestHandlerClass().newInstance();
					handler.initialize(request, response);

					if ("true".equalsIgnoreCase(Settings.SELF.getProperty("menton.logging.writeHttpRequest"))) {
						logger.info(request.toString());
					}

					response.setContent(handler.service());
				}
				catch (InstantiationException | IllegalAccessException | URISyntaxException e) {
					logger.error(e.getMessage(), e);

					response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
					response.setContent(e.getMessage());
				}
			}
			catch (URISyntaxException e) {
				logger.error(e.getMessage(), e);

				response.setStatus(HttpResponseStatus.BAD_REQUEST);
				response.setContent(e.getMessage());
			}
		}

		setDefaultHeaders(httpRequest, response);

		if ("true".equalsIgnoreCase(Settings.SELF.getProperty("menton.logging.writeHttpResponse"))) {
			logger.info(response.toString());
		}

		return response;
	}

	private void setDefaultHeaders(FullHttpRequest request, HttpResponse response) {

		response.headers().add(HttpHeaderNames.SERVER, Settings.SELF.getProperty("menton.versoin"));

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
}
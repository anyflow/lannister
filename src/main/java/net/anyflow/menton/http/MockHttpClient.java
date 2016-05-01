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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class MockHttpClient implements IHttpClient {

	private static final Logger logger = LoggerFactory.getLogger(MockHttpClient.class);

	private final HttpRequest httpRequest;
	private final MockHttpServer mockServer;

	public MockHttpClient(MockHttpServer mockServer, String uri) throws URISyntaxException {
		this.httpRequest = new HttpRequest(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri));
		this.mockServer = mockServer;

		if (httpRequest().uriObject().getScheme().equalsIgnoreCase("http") == false) {
			String message = "HTTP is supported only.";
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

	@Override
	public <T> IHttpClient setOption(ChannelOption<T> option, T value) {
		// TODO Auto-generated method stub
		return null;
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
		setDefaultHeaders();

		if (logger.isDebugEnabled()) {
			logger.debug(httpRequest().toString());
		}

		HttpResponse response = mockServer.service(httpRequest());
		if (receiver != null) {
			receiver.messageReceived(httpRequest(), response);
			return null;
		}
		else {
			return response;
		}
	}

	private void setDefaultHeaders() {
		if (httpRequest().headers().contains(HttpHeaderNames.HOST) == false) {
			httpRequest().headers().set(HttpHeaderNames.HOST, httpRequest().uriObject().getHost());
		}
		if (httpRequest().headers().contains(HttpHeaderNames.CONNECTION) == false) {
			httpRequest().headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		}
		if (httpRequest().headers().contains(HttpHeaderNames.ACCEPT_ENCODING) == false) {
			httpRequest().headers().set(HttpHeaderNames.ACCEPT_ENCODING,
					HttpHeaderValues.GZIP + ", " + HttpHeaderValues.DEFLATE);
		}
		if (httpRequest().headers().contains(HttpHeaderNames.ACCEPT_CHARSET) == false) {
			httpRequest().headers().set(HttpHeaderNames.ACCEPT_CHARSET, "utf-8");
		}
		if (httpRequest().headers().contains(HttpHeaderNames.CONTENT_TYPE) == false) {
			httpRequest().headers().set(HttpHeaderNames.CONTENT_TYPE,
					HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);
		}
	}
}

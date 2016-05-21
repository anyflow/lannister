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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * @author anyflow
 */
public class HttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

	private static final Logger logger = LoggerFactory.getLogger(HttpClientHandler.class);

	private final MessageReceiver receiver;
	private final HttpRequest request;
	private HttpResponse response;

	public HttpClientHandler(MessageReceiver receiver, HttpRequest request) {
		this.receiver = receiver;
		this.request = request;
	}

	public HttpResponse httpResponse() {
		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty
	 * .channel.ChannelHandlerContext, java.lang.Object)
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
		if (!ctx.channel().isActive()) { return; }

		response = HttpResponse.createFrom(msg, ctx.channel());

		if (logger.isDebugEnabled()) {
			logger.debug(response.toString());
		}

		if (receiver != null) {
			receiver.messageReceived(request, response);
		}

		ctx.channel().close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getMessage(), cause);
		ctx.channel().close();
	}
}
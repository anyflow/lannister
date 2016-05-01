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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public abstract class WebsocketFrameHandler extends MessageToMessageDecoder<WebSocketFrame> {

	public static final List<String> DEFAULT_SUBPROTOCOLS = null;
	public static final boolean ALLOW_EXTENSIONS = false;
	public static final int MAX_FRAME_SIZE = 65536;

	public abstract String subprotocols();

	public abstract String websocketPath();

	public abstract boolean allowExtensions();

	public abstract int maxFrameSize();

	public abstract void websocketFrameReceived(ChannelHandlerContext ctx, WebSocketFrame wsframe);

	@Override
	protected void decode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) throws Exception {
		websocketFrameReceived(ctx, msg);
	}
}
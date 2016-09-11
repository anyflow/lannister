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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.CharsetUtil;
import net.anyflow.lannister.Settings;

public class HttpResponse extends DefaultFullHttpResponse {

	private static final Logger logger = LoggerFactory.getLogger(HttpResponse.class);

	public static HttpResponse createServerDefault(String requestCookie) {
		HttpResponse ret = new HttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.buffer());

		ret.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");

		if (requestCookie == null) { return ret; }

		Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(requestCookie);
		if (cookies.isEmpty()) { return ret; }

		// Reset the cookies if necessary.
		for (Cookie cookie : cookies) {
			ret.headers().add(HttpHeaderNames.SET_COOKIE, ClientCookieEncoder.STRICT.encode(cookie));
		}

		return ret;
	}

	public static HttpResponse createFrom(FullHttpResponse source, Channel channel) {
		HttpResponse ret = new HttpResponse(source.protocolVersion(), source.status(), source.content().copy());

		ret.headers().set(source.headers());
		ret.trailingHeaders().set(source.trailingHeaders());

		return ret;
	}

	private HttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content) {
		super(version, status, content);
	}

	public void setContent(String content) {
		content().writeBytes(Strings.nullToEmpty(content).getBytes(CharsetUtil.UTF_8));
		logger.debug(content().toString(CharsetUtil.UTF_8));
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();

		buf.append("\r\n");
		buf.append("HTTP Status: " + this.status()).append("\r\n");
		buf.append("Version: " + this.protocolVersion()).append("\r\n");
		buf.append("Response Headers: ").append("\r\n");

		if (!this.headers().isEmpty()) {
			for (String name : this.headers().names()) {
				for (String value : this.headers().getAll(name)) {
					buf.append("   ").append(name).append(" = ").append(value).append("\r\n");
				}
			}
		}

		if (!Settings.INSTANCE.getBoolean("lannister.web.logging.logWebResourceHttpResponseContent", false)
				&& Settings.INSTANCE.webResourceExtensionToMimes()
						.containsValue(headers().get(HttpHeaderNames.CONTENT_TYPE))) {
			buf.append("Content: WEB RESOURCE CONTENT");
			return buf.toString();
		}

		String content = this.content().toString(CharsetUtil.UTF_8);

		int size = Settings.INSTANCE.getInt("lannister.web.logging.httpResponseContentSize", 100);

		if (size < 0) {
			buf.append("Content:\r\n   ").append(content);
		}
		else {
			int index = content.length() < size ? content.length() : size - 1;
			buf.append("The first " + size + " character(s) of response content:\r\n   ")
					.append(content.substring(0, index));
		}

		return buf.toString();
	}
}
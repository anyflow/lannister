/**
 * 
 */
package net.anyflow.menton.http;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

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

/**
 * @author anyflow
 */
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

	/**
	 * @param version
	 * @param status
	 */
	private HttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content) {
		super(version, status, content);
	}

	public void setContent(String content) {
		if (content == null) {
			content = "";
		}

		content().writeBytes(content.getBytes(CharsetUtil.UTF_8));
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

		if ("false".equalsIgnoreCase(
				Settings.SELF.getProperty("menton.logging.logWebResourceHttpResponseContent", "false"))
				&& Settings.SELF.webResourceExtensionToMimes()
						.containsValue(headers().get(HttpHeaderNames.CONTENT_TYPE))) {
			buf.append("Content: WEB RESOURCE CONTENT");
			return buf.toString();
		}

		String content = this.content().toString(CharsetUtil.UTF_8);

		int size = Ints.tryParse(Settings.SELF.getProperty("menton.logging.httpResponseContentSize", "100"));

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
/**
 * 
 */
package net.anyflow.menton.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.CharsetUtil;

/**
 * @author anyflow
 */
public class HttpRequest extends DefaultFullHttpRequest {

	private static final Logger logger = LoggerFactory.getLogger(HttpRequest.class);

	private final Map<String, List<String>> parameters;
	private final Map<String, String> pathParameters;
	private final URI uri;
	private final Set<Cookie> cookies;

	protected HttpRequest(FullHttpRequest fullHttpRequest) throws URISyntaxException {
		this(fullHttpRequest, new HashMap<String, String>());
	}

	protected HttpRequest(FullHttpRequest fullHttpRequest, Map<String, String> pathParameters)
			throws URISyntaxException {
		super(fullHttpRequest.protocolVersion(), fullHttpRequest.method(), fullHttpRequest.uri(),
				fullHttpRequest.content().copy());

		this.headers().set(fullHttpRequest.headers());
		this.trailingHeaders().set(fullHttpRequest.trailingHeaders());
		this.setDecoderResult(fullHttpRequest.decoderResult());
		this.uri = createUriWithNormalizing(fullHttpRequest.uri());

		this.parameters = parameters();
		this.cookies = cookies();
		this.pathParameters = pathParameters;
	}

	private URI createUriWithNormalizing(String uri) throws URISyntaxException {
		URI temp = new URI(uri);

		String scheme = temp.getScheme();
		if (scheme == null) {
			scheme = "http";
		}

		int port = temp.getPort();
		if (port == -1) {
			if (scheme.equalsIgnoreCase("http")) {
				port = 80;
			}
			else if (scheme.equalsIgnoreCase("https")) {
				port = 443;
			}
			else {
				throw new URISyntaxException(uri, "Invalid protocol.");
			}
		}

		return new URI(scheme, temp.getUserInfo(), temp.getHost(), port, temp.getPath(), temp.getQuery(),
				temp.getFragment());
	}

	public Set<Cookie> cookies() {
		if (cookies != null) { return cookies; }

		String cookie = headers().get(HttpHeaderNames.COOKIE);
		if (cookie == null || "".equals(cookie)) { return new HashSet<Cookie>(); }

		Set<Cookie> ret = ServerCookieDecoder.STRICT.decode(cookie);

		return ret == null || ret.isEmpty() ? new HashSet<Cookie>() : ret;
	}

	public Map<String, String> pathParameters() {
		return pathParameters;
	}

	public String pathParameter(String name) {
		return pathParameters.get(name);
	}

	public Map<String, List<String>> parameters() {

		if (parameters != null) { return parameters; }

		Map<String, List<String>> ret = Maps.newHashMap();

		if (HttpMethod.GET.equals(method()) || HttpMethod.DELETE.equals(method())) {
			ret.putAll((new QueryStringDecoder(uri())).parameters());
			return ret;
		}
		else if (headers().contains(HttpHeaderNames.CONTENT_TYPE)
				&& headers().get(HttpHeaderNames.CONTENT_TYPE)
						.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())
				&& (HttpMethod.POST.equals(method()) || HttpMethod.PUT.equals(method()))) {

			ret.putAll((new QueryStringDecoder("/dummy?" + content().toString(CharsetUtil.UTF_8))).parameters());
		}

		return ret;
	}

	/**
	 * Get single parameter. In case of multiple values, the method returns the
	 * first.
	 * 
	 * @param name
	 *            parameter name.
	 * @return The first value of the parameter name. If it does not exist, it
	 *         returns null.
	 */
	public String parameter(String name) {

		if (parameters().containsKey(name) == false || parameters.get(name).size() <= 0) { return null; }

		return parameters().get(name).get(0);
	}

	public HttpRequest addParameter(String name, String value) {

		List<String> values = parameters().get(name);
		if (values == null) {
			values = new ArrayList<String>();
			values.add(value);
			parameters().put(name, values);
		}
		else {
			values.clear();
			values.add(value);
		}

		return this;
	}

	@Override
	public String toString() {

		StringBuilder buf = new StringBuilder();

		buf.append("\r\n");
		buf.append("Request URI: ").append(this.uri()).append("\r\n");
		buf.append("HTTP METHOD: ").append(this.method().toString()).append("\r\n");
		buf.append("Version: ").append(this.protocolVersion()).append("\r\n");
		buf.append("Request Headers:").append("\r\n");

		List<Entry<String, String>> headers = this.headers().entries();
		if (!headers.isEmpty()) {
			for (Entry<String, String> h : this.headers().entries()) {
				String key = h.getKey();
				String value = h.getValue();
				buf.append("   ").append(key).append(" = ").append(value).append("\r\n");
			}
		}

		Map<String, List<String>> params = parameters();

		buf.append("Query String Parameters: ");

		if (params.isEmpty()) {
			buf.append("NONE\r\n");
		}
		else {
			for (Entry<String, List<String>> p : params.entrySet()) {
				String key = p.getKey();
				List<String> vals = p.getValue();
				for (String val : vals) {
					buf.append("\r\n   ").append(key).append(" = ").append(val).append("\r\n");
				}
			}
		}

		buf.append("Content: ");
		if (this.content().isReadable()) {
			buf.append("\r\n   ").append(content().toString(CharsetUtil.UTF_8));
		}
		else {
			buf.append("UNREADABLE CONTENT or NONE");
		}

		DecoderResult result = this.decoderResult();

		if (result.isSuccess() == false) {
			buf.append("\r\n").append(".. WITH DECODER FAILURE:");
			buf.append("\r\n   ").append(result.cause());
		}

		return buf.toString();
	}

	public void setContent(String content) {
		content = Strings.nullToEmpty(content);

		byte[] contentByte = content.getBytes(CharsetUtil.UTF_8);
		headers().set(HttpHeaderNames.CONTENT_LENGTH, contentByte.length);
		content().writeBytes(contentByte);
		logger.debug(content().toString(CharsetUtil.UTF_8));
	}

	public URI uriObject() {
		return uri;
	}

	protected void normalize() {
		normalizeParameters();

		String encoded = ClientCookieEncoder.STRICT.encode(cookies);
		if (encoded == null) { return; }

		headers().set(HttpHeaderNames.COOKIE, encoded);
	}

	private String convertParametersToString() {

		StringBuilder builder = new StringBuilder();

		for (String name : parameters().keySet()) {

			for (String value : parameters().get(name)) {
				builder = builder.append(name).append("=").append(value).append("&");
			}
		}

		String ret = builder.toString();
		if (ret.length() <= 0) { return ""; }

		if (ret.charAt(ret.length() - 1) == '&') {
			return ret.substring(0, ret.length() - 1);
		}
		else {
			return ret;
		}
	}

	private void normalizeParameters() {
		String address = (new StringBuilder()).append(uriObject().getScheme()).append("://")
				.append(uriObject().getAuthority()).append(uriObject().getPath()).toString();

		if (HttpMethod.GET.equals(method()) || HttpMethod.DELETE.equals(method())) {
			String parameters = convertParametersToString();
			address += Strings.isNullOrEmpty(parameters) ? "" : "?" + parameters;
		}
		else if ((HttpMethod.POST.equals(method()) || HttpMethod.PUT.equals(method()))
				&& (headers().contains(HttpHeaderNames.CONTENT_TYPE) == false
						|| headers().get(HttpHeaderNames.CONTENT_TYPE)
								.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()))) {
			ByteBuf content = Unpooled.copiedBuffer(convertParametersToString(), CharsetUtil.UTF_8);

			headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
			content().clear();
			content().writeBytes(content);
		}

		setUri(address);
	}
}
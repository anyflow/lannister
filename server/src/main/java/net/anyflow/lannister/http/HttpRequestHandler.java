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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.reflections.Reflections;

import com.google.common.collect.Maps;

import net.anyflow.lannister.Settings;

public abstract class HttpRequestHandler {

	public static final String NO_WEB_RESOURCE_PATH = "none";

	private static final Map<String, Class<? extends HttpRequestHandler>> HANDLER_CLASS_MAP = Maps.newHashMap();
	private static Set<Class<? extends HttpRequestHandler>> REQUEST_HANDLER_CLASSES;
	private static String REQUEST_HANDLER_PACKAGE_ROOT;

	private HttpRequest request;
	private HttpResponse response;

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface Handles {
		String[] paths();

		String[] httpMethods();

		String webResourcePath() default NO_WEB_RESOURCE_PATH;
	}

	public abstract String service();

	protected HttpRequestHandler initialize(HttpRequest request, HttpResponse response) throws URISyntaxException {
		this.request = request;
		this.response = response;

		return this;
	}

	public HttpRequest httpRequest() {
		return request;
	}

	public HttpResponse httpResponse() {
		return response;
	}

	public static void setRequestHandlerPakcageRoot(String requestHandlerPakcageRoot) {
		REQUEST_HANDLER_PACKAGE_ROOT = requestHandlerPakcageRoot;
	}

	protected static MatchedCriterion findRequestHandler(String requestedPath, String httpMethod) {
		for (Map.Entry<String, Class<? extends HttpRequestHandler>> item : HANDLER_CLASS_MAP.entrySet()) {
			MatchedCriterion mc = match(requestedPath, httpMethod, item.getKey());

			if (mc.matched) {
				mc.requestHandlerClass = HANDLER_CLASS_MAP.get(item.getKey());
				return mc;
			}
		}

		if (REQUEST_HANDLER_CLASSES == null) {
			REQUEST_HANDLER_CLASSES = new Reflections(REQUEST_HANDLER_PACKAGE_ROOT)
					.getSubTypesOf(HttpRequestHandler.class);
		}

		final ReturnWrapper wrapper = new ReturnWrapper();
		REQUEST_HANDLER_CLASSES.stream().anyMatch(c -> {
			HttpRequestHandler.Handles annotation = c.getAnnotation(HttpRequestHandler.Handles.class);
			if (annotation == null) { return false; }

			return Stream.of(annotation.httpMethods()).anyMatch(m -> {
				if (!m.equalsIgnoreCase(httpMethod)) { return false; }

				return Stream.of(annotation.paths()).anyMatch(p -> {
					return wrapper.setValueIfMatch(requestedPath, c, m, p);
				});
			});
		});

		return wrapper.value == null ? new MatchedCriterion() : wrapper.value;
	}

	private static class ReturnWrapper {
		private MatchedCriterion value = null;

		private boolean setValueIfMatch(String requestedPath, Class<? extends HttpRequestHandler> handlerClass,
				String methodToCheck, String pathToCheck) {
			String path = (pathToCheck.charAt(0) == '/') ? pathToCheck
					: Settings.INSTANCE.httpContextRoot() + pathToCheck;
			String criterion = path + "/" + methodToCheck;

			MatchedCriterion mc = match(requestedPath, methodToCheck, criterion);

			if (mc.matched) {
				HANDLER_CLASS_MAP.put(criterion, handlerClass);
				mc.requestHandlerClass = handlerClass;
				this.value = mc;

				return true;
			}
			else {
				return false;
			}
		};
	}

	protected static class MatchedCriterion {
		private boolean matched = false;
		private Class<? extends HttpRequestHandler> requestHandlerClass;
		private String criterionPath;
		private String criterionHttpMethod;
		private final Map<String, String> pathParameters = Maps.newHashMap();

		public Class<? extends HttpRequestHandler> requestHandlerClass() {
			return requestHandlerClass;
		}

		public String criterionPath() {
			return criterionPath;
		}

		public String criterionHttpMethod() {
			return criterionHttpMethod;
		}

		public Map<String, String> pathParameters() {
			return pathParameters;
		}
	}

	private static MatchedCriterion match(String requestedPath, String httpMethod, String criterion) {
		MatchedCriterion ret = new MatchedCriterion();

		String testTarget = requestedPath + "/" + httpMethod;

		String[] testTokens = testTarget.split("/");
		String[] criterionTokens = criterion.split("/");

		if (criterionTokens.length != testTokens.length) { return ret; }

		// should start with #1 due to item[0] is whitespace.
		for (int i = 1; i < criterionTokens.length; ++i) {
			if (criterionTokens[i].startsWith("{") && criterionTokens[i].endsWith("}")) {
				ret.pathParameters.put(criterionTokens[i].substring(1, criterionTokens[i].length() - 1), testTokens[i]);
			}
			else if (!criterionTokens[i].equalsIgnoreCase(testTokens[i])) { return ret; }
		}

		ret.matched = true;
		ret.criterionHttpMethod = httpMethod;
		ret.criterionPath = criterion.replace("/" + httpMethod, "");

		return ret;
	}
}
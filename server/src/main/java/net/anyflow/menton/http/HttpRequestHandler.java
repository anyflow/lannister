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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

import com.google.common.collect.Maps;

import net.anyflow.lannister.Settings;

/**
 * Base class for request handler. The class contains common stuffs for
 * generating business logic.
 * 
 * @author anyflow
 */
public abstract class HttpRequestHandler {

	private static final Map<String, Class<? extends HttpRequestHandler>> handlerClassMap = Maps.newHashMap();
	private static Set<Class<? extends HttpRequestHandler>> requestHandlerClasses;
	private static String requestHandlerPakcageRoot;

	private HttpRequest request;
	private HttpResponse response;

	/**
	 * @author anyflow
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface Handles {

		/**
		 * @return supported paths
		 */
		String[] paths();

		/**
		 * supported http methods
		 * 
		 * @return http method string
		 */
		String[] httpMethods();

		String webResourcePath() default "none";
	}

	/**
	 * @return processed response body string
	 */
	public abstract String service();

	protected void initialize(HttpRequest request, HttpResponse response) throws URISyntaxException {
		this.request = request;
		this.response = response;
	}

	public HttpRequest httpRequest() {
		return request;
	}

	public HttpResponse httpResponse() {
		return response;
	}

	public static void setRequestHandlerPakcageRoot(String requestHandlerPakcageRoot) {
		HttpRequestHandler.requestHandlerPakcageRoot = requestHandlerPakcageRoot;
	}

	protected static MatchedCriterion findRequestHandler(String requestedPath, String httpMethod) {
		for (String criterion : handlerClassMap.keySet()) {
			MatchedCriterion mc = match(requestedPath, httpMethod, criterion);

			if (mc.result == true) {
				mc.requestHandlerClass = handlerClassMap.get(criterion);
				return mc;
			}
		}

		if (requestHandlerClasses == null) {
			requestHandlerClasses = (new Reflections(requestHandlerPakcageRoot))
					.getSubTypesOf(HttpRequestHandler.class);
		}

		MatchedCriterion ret = null;
		for (Class<? extends HttpRequestHandler> item : requestHandlerClasses) {
			HttpRequestHandler.Handles annotation = item.getAnnotation(HttpRequestHandler.Handles.class);

			if (annotation == null) {
				continue;
			}

			for (String method : annotation.httpMethods()) {
				if (method.equalsIgnoreCase(httpMethod) == false) {
					continue;
				}

				for (String rawPath : annotation.paths()) {
					String path = (rawPath.charAt(0) == '/') ? rawPath : Settings.SELF.httpContextRoot() + rawPath;
					String criterion = path + "/" + method;

					MatchedCriterion mc = match(requestedPath, method, criterion);

					if (mc.result == true) {
						handlerClassMap.put(criterion, item);
						mc.requestHandlerClass = item;

						ret = mc;
						break;
					}
				}
			}
		}

		return ret == null ? new MatchedCriterion() : ret;
	}

	protected static class MatchedCriterion {
		private boolean result;
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
			else if (criterionTokens[i].equalsIgnoreCase(testTokens[i]) == false) { return ret; }
		}

		ret.result = true;
		ret.criterionHttpMethod = httpMethod;
		ret.criterionPath = criterion.replace("/" + httpMethod, "");

		return ret;
	}
}
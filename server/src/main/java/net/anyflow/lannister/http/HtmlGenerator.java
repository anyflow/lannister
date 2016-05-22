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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.tidy.Tidy;

import io.netty.handler.codec.http.HttpResponseStatus;

public class HtmlGenerator {

	private static final Logger logger = LoggerFactory.getLogger(HtmlGenerator.class);

	private static String html_error;

	static {
		html_error = tidy(Thread.currentThread().getContextClassLoader().getResourceAsStream("html/error.htm"));
	}

	public static String generate(Map<String, String> values, String htmlPath) throws IOException {
		return replace(values, tidy(Thread.currentThread().getContextClassLoader().getResourceAsStream(htmlPath)));
	}

	private static String replace(Map<String, String> values, String htmlTemplate) {
		String openMarker = "${";
		String closeMarker = "}";

		String ret = htmlTemplate;

		for (Map.Entry<String, String> item : values.entrySet()) {
			ret = ret.replace(openMarker + item.getKey() + closeMarker, item.getValue());
		}

		return ret;
	}

	public static String error(String message, HttpResponseStatus status) {
		HashMap<String, String> values = new HashMap<String, String>();

		values.put("ERROR_CODE", status.toString());
		values.put("message", message);

		return replace(values, html_error);
	}

	private static String tidy(InputStream is) {
		Tidy tidy = new Tidy();

		tidy.setQuiet(true);
		tidy.setDocType("loose");
		tidy.setTidyMark(false);
		tidy.setOutputEncoding("UTF8");

		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		tidy.parse(is, out);

		try {
			return out.toString("UTF-8");
		}
		catch (UnsupportedEncodingException e) {

			logger.error(e.getMessage(), e);
			return null;
		}
	}
}
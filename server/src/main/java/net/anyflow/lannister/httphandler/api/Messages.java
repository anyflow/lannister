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

package net.anyflow.lannister.httphandler.api;

import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import io.netty.handler.codec.http.HttpResponseStatus;
import net.anyflow.lannister.http.HttpRequestHandler;
import net.anyflow.lannister.message.Message;

@HttpRequestHandler.Handles(paths = { "api/messages" }, httpMethods = { "GET" })
public class Messages extends HttpRequestHandler {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Messages.class);

	@Override
	public String service() {
		String filter = Strings.nullToEmpty(httpRequest().parameter("filter"));

		switch (filter) {
		case "":
		case "all":
			return all();

		default:
			return null;
		}
	}

	private String all() {
		try {
			return new ObjectMapper().writeValueAsString(
					Message.NEXUS.keySet().stream().map(key -> Message.NEXUS.get(key)).collect(Collectors.toList()));
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);

			this.httpResponse().setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			return null;
		}
	}
}

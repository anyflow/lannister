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

package net.anyflow.lannister.httphandler.admin;

import net.anyflow.lannister.http.HttpRequestHandler;

@HttpRequestHandler.Handles(paths = { "/admin", "/admin/index", "/admin/dashboard", "/admin/messages", "/admin/clients",
		"/admin/broker", "/admin/credential", "/admin/websocket_tester",
		"/admin/about" }, httpMethods = { "GET" }, webResourcePath = "admin/index.html")
public class Index extends HttpRequestHandler {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Index.class);

	@Override
	public String service() {
		return null;
	}
}
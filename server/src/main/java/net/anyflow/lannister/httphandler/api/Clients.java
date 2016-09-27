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

import com.hazelcast.core.IdGenerator;

import net.anyflow.lannister.Hazelcast;
import net.anyflow.lannister.http.HttpRequestHandler;
import net.anyflow.lannister.session.Session;

@HttpRequestHandler.Handles(paths = { "api/clients" }, httpMethods = { "POST" })
public class Clients extends HttpRequestHandler {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Clients.class);

	@Override
	public String service() {
		IdGenerator idgen = Hazelcast.INSTANCE.getIdGenerator("CLIENT_ID_GENERATOR");

		String clientId = null;
		while (clientId == null) {
			clientId = String.format("laniClientId%011d", idgen.newId());

			if (Session.NEXUS.get(clientId) != null) {
				clientId = null;
			}
		}

		return String.format("{\"id\":\"%s\"}", clientId);
	}
}
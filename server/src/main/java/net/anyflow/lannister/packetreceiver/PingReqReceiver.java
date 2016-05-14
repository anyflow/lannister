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

package net.anyflow.lannister.packetreceiver;

import net.anyflow.lannister.message.MessageFactory;
import net.anyflow.lannister.session.Session;

public class PingReqReceiver {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PingReqReceiver.class);

	public static PingReqReceiver SHARED = new PingReqReceiver();

	private PingReqReceiver() {
	}

	protected void handle(Session session) {
		session.send(MessageFactory.pingresp()); // [MQTT-3.12.4-1]
	}
}
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
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.Topics.ClientType;

public class PubRelReceiver {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PubRelReceiver.class);

	public static PubRelReceiver SHARED = new PubRelReceiver();

	private PubRelReceiver() {
	}

	protected void handle(Session session, int messageId) {

		// TODO what if the PUBREL is resented one?

		Topic topic = Topic.NEXUS.get(session.clientId(), messageId, ClientType.PUBLISHER);
		if (topic == null) {
			logger.error("Topic does not exist : [clientId={}, messageId={}]", session.clientId(), messageId);
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}

		session.send(MessageFactory.pubcomp(messageId)).addListener(f -> {
			topic.removeInboundMessageStatus(session.clientId(), messageId);
			logger.debug("Inbound message status REMOVED : [clientId={}, messageId={}]", session.clientId(), messageId);
		});
	}
}
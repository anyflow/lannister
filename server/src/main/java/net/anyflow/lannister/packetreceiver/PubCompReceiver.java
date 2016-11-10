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

import io.netty.channel.ChannelHandlerContext;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.session.Session;

public class PubCompReceiver {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PubCompReceiver.class);

	public static final PubCompReceiver SHARED = new PubCompReceiver();

	private PubCompReceiver() {
	}

	protected void handle(ChannelHandlerContext ctx, Session session, int messageId) {
		OutboundMessageStatus status = OutboundMessageStatus.NEXUS.removeByKey(messageId, session.clientId());
		if (status == null) {
			logger.error("PUBCOMP target does not exist [clientId={}, messageId={}]", session.clientId(), messageId);
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}

		logger.debug("Outbound message status REMOVED [clientId={}, messageId={}]", session.clientId(), messageId);
	}
}
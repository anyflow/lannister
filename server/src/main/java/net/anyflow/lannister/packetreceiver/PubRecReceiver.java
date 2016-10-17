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

import io.netty.util.concurrent.GlobalEventExecutor;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.plugin.DeliveredEventArgs;
import net.anyflow.lannister.plugin.DeliveredEventListener;
import net.anyflow.lannister.plugin.Plugins;
import net.anyflow.lannister.session.Session;

public class PubRecReceiver {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PubRecReceiver.class);

	public static final PubRecReceiver SHARED = new PubRecReceiver();

	private PubRecReceiver() {
	}

	protected void handle(Session session, int messageId) {
		OutboundMessageStatus status = OutboundMessageStatus.NEXUS.getBy(messageId, session.clientId());
		if (status == null || status.status() == OutboundMessageStatus.Status.TO_PUBLISH) {
			logger.error("PUBREC target does not exist or Invalid status [clientId={}, messageId={}]",
					session.clientId(), messageId);
			session.dispose(true); // [MQTT-3.3.5-2]
			return;
		}

		if (status.status() == OutboundMessageStatus.Status.PUBLISHED) {
			GlobalEventExecutor.INSTANCE.execute(() -> {
				Plugins.INSTANCE.get(DeliveredEventListener.class).delivered(new DeliveredEventArgs() {
					@Override
					public String clientId() {
						return session.clientId();
					}

					@Override
					public int messageId() {
						return messageId;
					}
				});
			});
		}

		OutboundMessageStatus.NEXUS.update(messageId, session.clientId(), OutboundMessageStatus.Status.PUBRECED);

		session.send(MqttMessageFactory.pubrel(messageId), null);
	}
}
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

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import net.anyflow.lannister.session.Session;

@Sharable
public class DisconnectReceiver {
	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DisconnectReceiver.class);
	public static final DisconnectReceiver INSTANCE = new DisconnectReceiver();

	public static final DisconnectReceiver SHARED = new DisconnectReceiver();

	private DisconnectReceiver() {
	}

	protected void handle(ChannelHandlerContext ctx, Session session) {
		session.will(null); // [MQTT-3.1.2-8],[MQTT-3.1.2-10]
		session.dispose(false); // [MQTT-3.14.4-1],[MQTT-3.14.4-2],[MQTT-3.14.4-3]
	}
}
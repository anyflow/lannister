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

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.CharsetUtil;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.Statistics;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.topic.Topic;

public class SysPublisher extends ChannelInboundHandlerAdapter {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SysPublisher.class);

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		int interval = Settings.SELF.getInt("annister.sys.intervalSeconds", 2);
		String requesterId = Settings.SELF.getProperty("lannister.broker.id", "lannister_broker_id");

		ctx.executor().scheduleAtFixedRate(() -> {
			Statistics.SELF.data().entrySet().stream().forEach(e -> {
				byte[] msg = e.getValue().value().getBytes(CharsetUtil.UTF_8);

				Topic.NEXUS.publish(new Message(-1, e.getKey(), requesterId, msg, MqttQoS.AT_MOST_ONCE, false));
			});
		} , 0, interval, TimeUnit.SECONDS);
	}
}
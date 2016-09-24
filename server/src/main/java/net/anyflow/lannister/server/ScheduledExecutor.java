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

package net.anyflow.lannister.server;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.CharsetUtil;
import net.anyflow.lannister.Hazelcast;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.Statistics;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.topic.Topic;

public class ScheduledExecutor extends ChannelInboundHandlerAdapter {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScheduledExecutor.class);

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		int $sysPublisherInterval = Settings.INSTANCE.getInt("mqttserver.sys.intervalSeconds", 2);
		ctx.executor().scheduleAtFixedRate(new $SysPublisher(), 0, $sysPublisherInterval, TimeUnit.SECONDS);

		int sessionExpiratorInterval = Settings.INSTANCE
				.getInt("mqttserver.sessionExpirationHandlerExecutionIntervalSeconds", 0);
		ctx.executor().scheduleAtFixedRate(new SessionExpirator(), 0, sessionExpiratorInterval, TimeUnit.SECONDS);
	}

	static class $SysPublisher implements Runnable {
		@Override
		public void run() {
			Statistics.INSTANCE.data().entrySet().stream().forEach(e -> {
				byte[] msg = e.getValue().value().getBytes(CharsetUtil.UTF_8);

				Message message = new Message(-1, e.getKey(), Hazelcast.INSTANCE.currentId(), msg, MqttQoS.AT_MOST_ONCE,
						false);
				Topic topic = Topic.NEXUS.prepare(message);

				topic.publish(message);
			});
		}
	}

	static class SessionExpirator implements Runnable {
		@Override
		public void run() {
			List<Session> disposes = Lists.newArrayList();

			Session.NEXUS.ctxs().keySet().stream().filter(id -> {
				Session s = Session.NEXUS.get(id);
				return s.isExpired();
			}).forEach(id -> disposes.add(Session.NEXUS.get(id)));

			disposes.stream().forEach(s -> s.dispose(true)); // [MQTT-3.1.2-24]

			if (disposes.size() > 0) {
				logger.debug("SessionExpirationHandler executed [dispose count={}]", disposes.size());
			}
		}
	}
}
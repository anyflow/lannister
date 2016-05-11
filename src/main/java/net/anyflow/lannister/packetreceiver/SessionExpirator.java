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

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.session.Session;

public class SessionExpirator extends ChannelInboundHandlerAdapter {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SessionExpirator.class);

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

		int interval = Settings.SELF.getInt("lannister.sessionExpirationHandlerExecutionIntervalSeconds", 0);

		ctx.executor().scheduleWithFixedDelay(() -> {
			List<Session> disposes = Lists.newArrayList();

			Session.NEXUS.ctxs().keySet().stream().filter(id -> {
				Session s = Session.NEXUS.get(id);
				return s.isExpired();
			}).forEach(id -> disposes.add(Session.NEXUS.get(id)));

			disposes.stream().forEach(s -> s.dispose(true)); // [MQTT-3.1.2-24]

			logger.debug("SessionExpirationHandler executed : [dispose count={}]", disposes.size());

		} , 0, interval, TimeUnit.SECONDS);
	}
}
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

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import net.anyflow.lannister.plugin.DisconnectEventArgs;
import net.anyflow.lannister.plugin.DisconnectEventListener;
import net.anyflow.lannister.plugin.Plugins;
import net.anyflow.lannister.session.Session;

public class GenericReceiver extends SimpleChannelInboundHandler<MqttMessage> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GenericReceiver.class);

	private static final String UNKNOWN_RETURN_CODE = "unknown connect return code:";
	private static final String UNKNOWN_RETURN_TYPE = "Unknown message type: ";

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
		if (msg.decoderResult().isSuccess() == false) {
			logger.error("decoding MQTT message failed : {}", msg.decoderResult().cause().getMessage());

			Session session = Session.NEXUS.get(ctx.channel().id());
			if (session != null) {
				session.dispose(true); // [MQTT-4.8.0-1]
			}
			else {
				ctx.channel().writeAndFlush(msg).addListener(f -> {
					logger.debug("packet outgoing [{}]", msg);
					ctx.channel().disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-3.2.2-5]
				});
			}
			return;
		}
		else {
			logger.debug("packet incoming [message={}]", msg.toString());

			Session session = Session.NEXUS.get(ctx.channel().id());
			if (session == null) {
				logger.error("None exist session message : {}", msg.toString());
				ctx.disconnect().addListener(ChannelFutureListener.CLOSE); // [MQTT-4.8.0-1]
				return;
			}

			session.setLastIncomingTime(new Date());

			switch (msg.fixedHeader().messageType()) {
			case DISCONNECT:
				DisconnectReceiver.SHARED.handle(session);
				return;

			case PINGREQ:
				PingReqReceiver.SHARED.handle(session);
				return;

			case PUBREC:
				PubRecReceiver.SHARED.handle(session, ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId());
				return;

			case PUBREL:
				PubRelReceiver.SHARED.handle(session, ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId());
				return;

			case PUBCOMP:
				PubCompReceiver.SHARED.handle(session,
						((MqttMessageIdVariableHeader) msg.variableHeader()).messageId());
				return;

			default:
				session.dispose(true); // [MQTT-4.8.0-1]
				return;
			}
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Session session = Session.NEXUS.get(ctx.channel().id());
		if (session == null) {
			logger.debug("session does not exist : [channelId={}]", ctx.channel().id());
			return;
		}
		else {
			session.dispose(true); // abnormal disconnection without
									// DISCONNECT [MQTT-4.8.0-1]
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause.getMessage(), cause);

		if (cause instanceof DecoderException
				|| (cause instanceof IllegalArgumentException && contains(cause.getMessage()))) {
			Session session = Session.NEXUS.get(ctx.channel().id());
			if (session != null) {
				session.dispose(true);
			}
			else {
				ctx.channel().disconnect().addListener(ChannelFutureListener.CLOSE);

				Plugins.SELF.get(DisconnectEventListener.class).disconnected(new DisconnectEventArgs() {
					@Override
					public String clientId() {
						return null;
					}

					@Override
					public Boolean cleanSession() {
						return null;
					}

					@Override
					public Boolean byDisconnectMessage() {
						return false;
					}
				});
			}
		}
		else {
			super.exceptionCaught(ctx, cause);
		}
	}

	private static boolean contains(String message) {
		List<String> messages = Lists.newArrayList(UNKNOWN_RETURN_CODE, UNKNOWN_RETURN_TYPE);

		return messages.stream().anyMatch(s -> s.contains(message));
	}
}
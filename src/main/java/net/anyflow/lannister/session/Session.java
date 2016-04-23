package net.anyflow.lannister.session;

import java.util.Date;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.Maps;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;

public class Session implements MessageListener<MessageObject>, java.io.Serializable {

	// TODO An I/O error or network failure detected by the Server.
	// TODO The Client fails to communicate within the Keep Alive time.
	// TODO The Client closes the Network Connection without first sending a
	// DISCONNECT Packet.
	// TODO The Server closes the Network Connection because of a protocol
	// error.

	// TODO keepAlive checking

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);
	private static final long serialVersionUID = -1800874748722060393L;

	private static ExecutorService SERVICE = Executors.newCachedThreadPool();

	private static final int MAX_MESSAGE_ID_NUM = 0xffff;
	private static final int MIN_MESSAGE_ID_NUM = 1;

	private final String clientId;
	private ChannelHandlerContext ctx;
	private final Date createTime;
	private final ConcurrentMap<String, TopicRegister> topicRegisters;
	private int messageId;
	private Will will;
	private String retainedMessage;
	private boolean shouldPersist;
	private int keepAliveTimeSeconds;
	private Date lastIncomingTime;

	public Session(ChannelHandlerContext ctx, String clientId, int keepAliveTimeSeconds, boolean shouldPersist) {
		this.ctx = ctx;
		this.clientId = clientId;
		this.createTime = new Date();
		this.topicRegisters = Maps.newConcurrentMap();
		this.messageId = 0;
		this.keepAliveTimeSeconds = keepAliveTimeSeconds;
		this.lastIncomingTime = new Date();
		this.shouldPersist = shouldPersist;
	}

	public String clientId() {
		return clientId;
	}

	public ChannelHandlerContext ctx() {
		return ctx;
	}

	public Date createTime() {
		return createTime;
	}

	public Will will() {
		return will;
	}

	public void setWill(Will will) {
		this.will = will;
	}

	public String retainedMessage() {
		return retainedMessage;
	}

	public boolean shouldPersist() {
		return shouldPersist;
	}

	public Date lastIncomingTime() {
		return lastIncomingTime;
	}

	public boolean isExpired() {
		if (keepAliveTimeSeconds == 0) { return false; }

		return ((new Date()).getTime() - lastIncomingTime.getTime()) * 1000 < keepAliveTimeSeconds;
	}

	public void setLastIncomingTime(Date lastIncomingTime) {
		this.lastIncomingTime = lastIncomingTime;
	}

	public int keepAliveTimeSeconds() {
		return keepAliveTimeSeconds;
	}

	public ConcurrentMap<String, TopicRegister> topicRegisters() {
		return topicRegisters;
	}

	public void revive(ChannelHandlerContext ctx) {
		this.ctx = ctx;
		this.retainedMessage = null;
	}

	public int nextMessageId() {
		synchronized (this) {
			messageId = messageId + 1;

			if (messageId > MAX_MESSAGE_ID_NUM) {
				messageId = MIN_MESSAGE_ID_NUM;
			}

			return messageId;
		}
	}

	public boolean isConnected() {
		return ctx != null && ctx.channel().isActive();
	}

	public ChannelFuture send(MqttMessage message) {
		if (ctx == null || ctx.channel().isActive() == false) {
			logger.error("Message is not sent - Channel is inactive : {}", message);
			return null;
		}

		final String log = message.toString();
		return ctx.writeAndFlush(message).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				logger.debug("Message outgoing : {}", log);
			}
		});
	}

	public void dispose(boolean sendWill) {
		ctx.disconnect().addListener(ChannelFutureListener.CLOSE);

		if (shouldPersist) {
			Repository.SELF.sessions().put(this.clientId, this); // [MQTT-3.1.2-4]
		}

		for (String topicName : topicRegisters.keySet()) {
			Repository.SELF.topic(topicName).removeMessageListener(topicRegisters.get(topicName).registrationId());
		}

		// TODO send Will

		logger.debug("Session disposed. [clientId={}/channelId={}]", clientId, ctx.channel().id());

		this.ctx = null;
		this.retainedMessage = null; // [MQTT-3.1.2.7]
	}

	@Override
	public void onMessage(Message<MessageObject> message) {
		final MessageObject msg = message.getMessageObject();

		SERVICE.submit(new Runnable() {
			@Override
			public void run() {
				logger.debug("Event arrived : [clientId:{}/message:{}]", Session.this.clientId, msg.toString());

				// TODO QoS leveling

				MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE,
						false, 7 + msg.message().length);

				MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(msg.topicName(),
						nextMessageId());

				Session.this.send(
						new MqttPublishMessage(fixedHeader, variableHeader, Unpooled.wrappedBuffer(msg.message())));

			}
		});
	}
}
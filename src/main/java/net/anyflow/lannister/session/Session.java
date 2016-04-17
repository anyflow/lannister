package net.anyflow.lannister.session;

import java.util.Date;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.Maps;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;

public class Session implements MessageListener<MessageObject> {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);

	private static ExecutorService SERVICE = Executors.newCachedThreadPool();

	private final String clientId;
	private final ChannelHandlerContext ctx;
	private final Date createTime;
	private final ConcurrentMap<String, TopicRegister> topicRegisters;
	private int messageId;

	public Session(ChannelHandlerContext ctx, String clientId) {
		this.ctx = ctx;
		this.clientId = clientId;
		this.createTime = new Date();
		this.topicRegisters = Maps.newConcurrentMap();
		this.messageId = 0;
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

	public ConcurrentMap<String, TopicRegister> topicRegisters() {
		return topicRegisters;
	}

	public int nextMessageId() {
		synchronized (this) {
			messageId = messageId + 1;

			if (messageId > 0xffff) {
				messageId = 1;
			}

			return messageId;
		}
	}

	@Override
	public void onMessage(Message<MessageObject> message) {
		final MessageObject msg = message.getMessageObject();

		SERVICE.submit(new Runnable() {
			@Override
			public void run() {
				// TODO QoS leveling

				MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE,
						false, 7 + msg.message().length);

				MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(msg.topicName(),
						nextMessageId());

				ctx.channel().writeAndFlush(
						new MqttPublishMessage(fixedHeader, variableHeader, Unpooled.wrappedBuffer(msg.message())));

				logger.debug("onMessage execution finished : {}", msg.toString());
			}
		});
	}
}
package net.anyflow.lannister.session;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.google.common.collect.Maps;
import com.hazelcast.core.MessageListener;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.messagehandler.MessageFactory;

public class Session implements MessageListener<Message>, java.io.Serializable {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);
	private static final long serialVersionUID = -1800874748722060393L;

	private static ExecutorService SERVICE = Executors.newCachedThreadPool();

	private static final int MAX_MESSAGE_ID_NUM = 0xffff;
	private static final int MIN_MESSAGE_ID_NUM = 1;

	private final String clientId;
	private ChannelHandlerContext ctx;
	private final Date createTime;
	private final Map<String, SessionTopic> topics;
	private int messageId;
	private Will will;
	private boolean shouldPersist;
	private int keepAliveTimeSeconds;
	private Date lastIncomingTime;

	public Session(ChannelHandlerContext ctx, String clientId, int keepAliveTimeSeconds, boolean shouldPersist) {
		this.ctx = ctx;
		this.clientId = clientId;
		this.createTime = new Date();
		this.topics = Maps.newConcurrentMap();
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

	public Map<String, SessionTopic> topics() {
		return topics;
	}

	public void revive(ChannelHandlerContext ctx) {
		this.ctx = ctx;
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

		if (shouldPersist == false) {
			Repository.SELF.sessions().remove(this.clientId);
		}

		for (String topicName : topics.keySet()) {
			Repository.SELF.topic(topicName).removeMessageListener(topics.get(topicName).registrationId());
		}

		// TODO send Will

		logger.debug("Session disposed. [clientId={}/channelId={}]", clientId, ctx.channel().id());

		this.ctx = null;

		for (SessionTopic topic : topics.values()) {
			topic.setRetainedMesage(null); // [MQTT-3.1.2.7]
		}
	}

	@Override
	public void onMessage(com.hazelcast.core.Message<Message> rawMessage) {
		final Message message = rawMessage.getMessageObject();

		SERVICE.submit(new Runnable() {
			@Override
			public void run() {
				logger.debug("Event arrived : [clientId:{}/message:{}]", Session.this.clientId, message.toString());

				SessionTopic topic = topics.get(message.topicName());

				if (message.qos().value() > 0) {
					topic.messages().put(message.id(), message);
				}

				if (message.isRetain()) {
					if (message.message().length > 0) {
						topic.setRetainedMesage(message);
					}
					else {
						topic.setRetainedMesage(null); // [MQTT-3.3.1-10],[MQTT-3.3.1-11]
					}
				}
				else {
					// do nothing [MQTT-3.3.1-12]
				}

				if (isConnected() == false) { return; }

				MqttQoS qos = topic.qos().value() <= message.qos().value() ? topic.qos() : message.qos();
				boolean isDuplicated = false; // [MQTT-3.3.1-2], [MQTT-3.3.1-3]
				boolean isRetain = false; // [MQTT-3.3.1-9]

				MqttPublishMessage publish = MessageFactory.publish(message, isDuplicated, qos, isRetain,
						nextMessageId());

				Session.this.send(publish).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						message.setSent(true);
					}
				});

			}
		});
	}

	public void publishUnackedMessages() {
		for (String key : topics.keySet()) {
			final Map<Integer, Message> messages = topics.get(key).messages();
			final SessionTopic topic = topics.get(key);

			messages.keySet().stream().sorted().forEach(new Consumer<Integer>() {
				@Override
				public void accept(Integer t) {
					Message message = messages.get(t);

					MqttQoS qos = topic.qos().value() <= message.qos().value() ? topic.qos() : message.qos();
					boolean isDuplicated = message.isSent();
					boolean isRetain = false; // [MQTT-3.3.1-9]

					MqttPublishMessage publish = MessageFactory.publish(message, isDuplicated, qos, isRetain,
							nextMessageId());

					Session.this.send(publish).addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							message.setSent(true);
						}
					});
				}
			});
		}
	}
}
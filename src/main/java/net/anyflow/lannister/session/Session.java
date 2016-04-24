package net.anyflow.lannister.session;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hazelcast.core.MessageListener;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.Jsonizable;
import net.anyflow.lannister.Literals;
import net.anyflow.lannister.admin.command.Sessions;
import net.anyflow.lannister.messagehandler.MessageFactory;

public class Session extends Jsonizable implements MessageListener<Message>, java.io.Serializable {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);
	private static final long serialVersionUID = -1800874748722060393L;

	private static ExecutorService SERVICE = Executors.newCachedThreadPool();

	private static final int MAX_MESSAGE_ID_NUM = 0xffff;
	private static final int MIN_MESSAGE_ID_NUM = 1;

	private transient ChannelHandlerContext ctx;

	@JsonProperty
	private final String clientId;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	private final Date createTime;
	@JsonProperty
	private final Map<String, SessionTopic> topics;
	@JsonProperty
	private final Map<Integer, Message> messages;
	@JsonProperty
	private int currentMessageId;
	@JsonProperty
	private Will will;
	@JsonProperty
	private boolean cleanSession;
	@JsonProperty
	private int keepAliveSeconds;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	private Date lastIncomingTime;

	public Session(ChannelHandlerContext ctx, String clientId, int keepAliveSeconds, boolean cleanSession) {
		this.ctx = ctx;
		this.clientId = clientId;
		this.createTime = new Date();
		this.topics = Maps.newConcurrentMap();
		this.messages = Maps.newConcurrentMap();
		this.currentMessageId = 0;
		this.keepAliveSeconds = keepAliveSeconds;
		this.lastIncomingTime = new Date();
		this.cleanSession = cleanSession;
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
		synchronize();
	}

	public boolean cleanSession() {
		return cleanSession;
	}

	public boolean shouldPersist() {
		return cleanSession;
	}

	public Date lastIncomingTime() {
		return lastIncomingTime;
	}

	public boolean isExpired() {
		if (keepAliveSeconds == 0) { return false; }

		return ((new Date()).getTime() - lastIncomingTime.getTime()) * 1000 < keepAliveSeconds;
	}

	public void setLastIncomingTime(Date lastIncomingTime) {
		this.lastIncomingTime = lastIncomingTime;
		synchronize();
	}

	public int keepAliveSeconds() {
		return keepAliveSeconds;
	}

	public ImmutableMap<Integer, Message> messages() {
		return ImmutableMap.copyOf(messages);
	}

	public Message removeMessage(int messageId) {
		Message ret = messages.remove(messageId);
		synchronize();

		return ret;
	}

	public ImmutableMap<String, SessionTopic> topics() {
		return ImmutableMap.copyOf(topics);
	}

	public void putTopic(String name, SessionTopic sessionTopic) {
		topics.put(name, sessionTopic);
		synchronize();
	}

	public SessionTopic removeTopic(String sessionTopic) {
		SessionTopic ret = topics.remove(sessionTopic);
		synchronize();

		return ret;
	}

	public void revive(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	public int nextMessageId() {
		synchronized (this) {
			currentMessageId = currentMessageId + 1;

			if (currentMessageId > MAX_MESSAGE_ID_NUM) {
				currentMessageId = MIN_MESSAGE_ID_NUM;
			}

			synchronize();

			return currentMessageId;
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
				logger.debug("packet outgoing : {}", log);
			}
		});
	}

	private void synchronize() {
		if (cleanSession) { return; }

		Repository.SELF.clientIdSessionMap().put(this.clientId, this);
	}

	public void dispose(boolean sendWill) {
		ctx.disconnect().addListener(ChannelFutureListener.CLOSE);

		if (cleanSession) {
			Repository.SELF.clientIdSessionMap().remove(this.clientId);
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
					messages.put(message.id(), message);
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

				if (isConnected() == false) {
					synchronize();
					return;
				}

				filter(message);

				MqttQoS qos = topic.qos().value() <= message.qos().value() ? topic.qos() : message.qos();
				boolean isDuplicated = false; // [MQTT-3.3.1-2], [MQTT-3.3.1-3]
				boolean isRetain = false; // [MQTT-3.3.1-9]

				MqttPublishMessage publish = MessageFactory.publish(message, isDuplicated, qos, isRetain,
						nextMessageId());

				Session.this.send(publish).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						message.setSent(true);
						synchronize();
					}
				});
			}
		});
	}

	private void filter(Message message) {
		if (message.topicName().startsWith("$") == false) { return; }

		if (message.topicName().equals("$COMMAND/GET/sessions")) {
			message.setMessage((new Sessions()).live());
		}
		else if (message.topicName().equals("$COMMAND/GET/sessions?filter=live")) {
			message.setMessage((new Sessions()).live());
		}
		else if (message.topicName().equals("$COMMAND/GET/sessions?filter=persisted")) {
			message.setMessage((new Sessions()).persisted());
		}
	}

	public void publishUnackedMessages() {
		for (String key : topics.keySet()) {
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
							synchronize();
						}
					});
				}
			});
		}
	}
}
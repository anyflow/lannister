package net.anyflow.lannister.session;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.mqtt.MqttMessage;
import net.anyflow.lannister.Jsonizable;
import net.anyflow.lannister.Literals;

public class Session extends Jsonizable implements java.io.Serializable {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);

	private static final long serialVersionUID = -1800874748722060393L;

	private static Sessions SESSIONS = new Sessions();

	private transient ChannelHandlerContext ctx;
	private transient Synchronizer synchronizer;
	private transient TopicSubscriber topicSubscriber;
	private transient MessageSender messageSender;
	private transient MessageListener messageListener;
	private transient SessionDisposer sessionDisposer;

	@JsonProperty
	private final String clientId;
	@JsonProperty
	private final Map<String, TopicSubscription> topicSubscriptions;
	@JsonProperty
	private final Map<Integer, Message> messages;
	@JsonProperty
	private int currentMessageId;
	@JsonProperty
	private Message will;
	@JsonProperty
	private boolean isCleanSession;
	@JsonProperty
	private int keepAliveSeconds;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	private final Date createTime;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	private Date lastIncomingTime;

	public Session(ChannelHandlerContext ctx, String clientId, int keepAliveSeconds, boolean cleanSession,
			Message will) {
		this.clientId = clientId;
		this.createTime = new Date();
		this.topicSubscriptions = Maps.newConcurrentMap();
		this.messages = Maps.newConcurrentMap();
		this.currentMessageId = 0;
		this.keepAliveSeconds = keepAliveSeconds;
		this.lastIncomingTime = new Date();
		this.isCleanSession = cleanSession;
		this.will = will; // [MQTT-3.1.2-9]

		revive(ctx);
	}

	public void revive(ChannelHandlerContext ctx) {
		this.ctx = ctx;
		this.synchronizer = new Synchronizer(this);
		this.topicSubscriber = new TopicSubscriber(clientId, topicSubscriptions, messages, synchronizer);
		this.messageSender = new MessageSender(ctx, topicSubscriber, messages, synchronizer);
		this.messageListener = new MessageListener(this, messages, synchronizer, messageSender, currentMessageId,
				ctx.executor());
		this.sessionDisposer = new SessionDisposer(ctx, clientId, will, messageSender);

		// TODO Do I must add listener to Repository.broadcaster?
	}

	public String clientId() {
		return clientId;
	}

	public ChannelId channelId() {
		return ctx.channel().id();
	}

	public Message will() {
		return will;
	}

	public boolean isCleanSession() {
		return isCleanSession;
	}

	public boolean isExpired() {
		if (keepAliveSeconds == 0) { return false; }

		return ((new Date()).getTime() - lastIncomingTime.getTime()) * 1000 < keepAliveSeconds;
	}

	public void setLastIncomingTime(Date lastIncomingTime) {
		this.lastIncomingTime = lastIncomingTime;
		synchronizer.execute();
	}

	public Message removeMessage(int messageId) {
		Message ret = messages.remove(messageId);
		synchronizer.execute();

		return ret;
	}

	public boolean isConnected() {
		return ctx != null && ctx.channel().isActive();
	}

	public ImmutableMap<String, TopicSubscription> topicSubscriptions() {
		return ImmutableMap.copyOf(topicSubscriptions);
	}

	public TopicSubscription[] matches(String topicName) {
		return topicSubscriber.matches(topicName);
	}

	public TopicSubscription putTopicSubscription(TopicSubscription topicSubscription) {
		return topicSubscriber.putTopicSubscription(topicSubscription);
	}

	public TopicSubscription removeTopicSubscription(final String topicFilter) {
		return topicSubscriber.removeTopicSubscription(topicFilter);
	}

	public ChannelFuture send(MqttMessage message) {
		return messageSender.send(message);
	}

	public Session persist() {
		return SESSIONS.persist(this);
	}

	public void dispose(boolean sendWill) {
		SESSIONS.remove(this, !sendWill);
		sessionDisposer.dispose(sendWill);
	}

	public void publishUnackedMessages() {
		messageSender.publishUnackedMessages();
	}

	public static Session getByClientId(String clientId, boolean includePersisted) {
		return SESSIONS.getByClientId(clientId, includePersisted);
	}

	public static Session getByChannelId(ChannelId channelId) {
		return SESSIONS.getByChannelId(channelId);
	}

	public static void put(Session session) {
		SESSIONS.put(session);
	}

	public static ImmutableMap<String, Session> clientIdMap(boolean includePersisted) {
		return SESSIONS.clientIdMap(includePersisted);
	}

	public static ImmutableMap<String, Session> persistedClientIdMap() {
		return SESSIONS.persistedClientIdMap();
	}

	protected void published(Message message) {
		messageListener.published(message);
	}

	protected static void topicAdded(Topic topic) {
		SESSIONS.topicAdded(topic);
	}
}
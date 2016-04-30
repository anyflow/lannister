package net.anyflow.lannister.session;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.mqtt.MqttMessage;
import net.anyflow.lannister.Literals;
import net.anyflow.lannister.Repository;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.serialization.Jsonizable;
import net.anyflow.lannister.serialization.SerializableFactory;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicSubscription;

public class Session extends Jsonizable implements com.hazelcast.nio.serialization.Portable {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);

	private static final int MAX_MESSAGE_ID_NUM = 0xffff;
	private static final int MIN_MESSAGE_ID_NUM = 1;

	public static Sessions NEXUS;
	public static final int ID = 4;

	private transient ChannelHandlerContext ctx;
	private transient Synchronizer synchronizer;
	private transient MessageSender messageSender;
	private transient SessionDisposer sessionDisposer;

	@JsonProperty
	private String clientId;
	@JsonProperty
	private Map<String, TopicSubscription> topicSubscriptions;
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
	private Date createTime;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	private Date lastIncomingTime;

	public Session() { // just for serialization
	}

	public Session(ChannelHandlerContext ctx, String clientId, int keepAliveSeconds, boolean cleanSession,
			Message will) {
		this.clientId = clientId;
		this.createTime = new Date();
		this.currentMessageId = 0;
		this.keepAliveSeconds = keepAliveSeconds;
		this.lastIncomingTime = new Date();
		this.isCleanSession = cleanSession;
		this.will = will; // [MQTT-3.1.2-9]
		this.topicSubscriptions = Repository.SELF.generator().getMap(topicSubscriptionsName());

		revive(ctx);
	}

	private String topicSubscriptionsName() {
		return "CLIENTID(" + clientId + ")_topicSubscriptions";
	}

	public void revive(ChannelHandlerContext ctx) {
		this.ctx = ctx;
		this.synchronizer = new Synchronizer(this);
		this.messageSender = new MessageSender(this, ctx);
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

	public boolean isConnected() {
		return ctx != null && ctx.channel().isActive();
	}

	public ImmutableMap<String, TopicSubscription> topicSubscriptions() {
		return ImmutableMap.copyOf(topicSubscriptions);
	}

	public Stream<TopicSubscription> matches(String topicName) {
		return topicSubscriptions.values().stream().filter(t -> t.isMatch(topicName));
	}

	public TopicSubscription putTopicSubscription(TopicSubscription topicSubscription) {
		TopicSubscription ret = topicSubscriptions.put(topicSubscription.topicFilter(), topicSubscription); // [MQTT-3.8.4-3]
		return ret;
	}

	public TopicSubscription removeTopicSubscription(final String topicFilter) {
		TopicSubscription ret = topicSubscriptions.remove(topicFilter);
		if (ret == null) { return null; }

		Topic.NEXUS.removeSubscribers(topicFilter, clientId);

		// TODO [MQTT-3.10.4-3] If a Server deletes a Subscription It MUST
		// complete the delivery of any QoS 1 or QoS 2 messages which it has
		// started to send to the Client.

		return ret;
	}

	public ChannelFuture send(MqttMessage message) {
		return messageSender.send(message);
	}

	public ChannelFuture sendPublish(Topic topic, Message message, boolean isRetain) {
		return messageSender.sendPublish(topic, message, isRetain);
	}

	public void dispose(boolean sendWill) {
		NEXUS.remove(this);
		sessionDisposer.dispose(sendWill);
	}

	public Stream<Topic> topics(Collection<TopicSubscription> topicSubscriptions) {
		return Topic.NEXUS.map().values().parallelStream().filter(t -> this.matches(t.name()).count() > 0);
	}

	public Stream<Topic> topics() {
		return topics(topicSubscriptions.values());
	}

	public int nextMessageId() {
		currentMessageId = currentMessageId + 1;

		if (currentMessageId > MAX_MESSAGE_ID_NUM) {
			currentMessageId = MIN_MESSAGE_ID_NUM;
		}

		synchronizer.execute();

		return currentMessageId;
	}

	public void completeRemainedMessages() {
		// TODO publish Unacked Messages
	}

	@JsonIgnore
	@Override
	public int getFactoryId() {
		return SerializableFactory.ID;
	}

	@JsonIgnore
	@Override
	public int getClassId() {
		return ID;
	}

	@Override
	public void writePortable(PortableWriter writer) throws IOException {
		writer.writeUTF("clientId", clientId);
		writer.writeInt("currentMessageId", currentMessageId);
		if (will != null) {
			writer.writePortable("will", will);
		}
		else {
			writer.writeNullPortable("will", SerializableFactory.ID, Message.ID);
		}
		writer.writeBoolean("isCleanSession", isCleanSession);
		writer.writeInt("keepAliveSeconds", keepAliveSeconds);
		writer.writeLong("createTime", createTime.getTime());
		writer.writeLong("lastIncomingTime", lastIncomingTime.getTime());
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		clientId = reader.readUTF("clientId");
		currentMessageId = reader.readInt("currentMessageId");
		will = reader.readPortable("will");
		isCleanSession = reader.readBoolean("isCleanSession");
		keepAliveSeconds = reader.readInt("keepAliveSeconds");
		createTime = new Date(reader.readLong("createTime"));
		lastIncomingTime = new Date(reader.readLong("lastIncomingTime"));

		topicSubscriptions = Repository.SELF.generator().getMap(topicSubscriptionsName());
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("clientId")
				.addIntField("currentMessageId").addPortableField("will", Message.classDefinition())
				.addBooleanField("isCleanSession").addIntField("keepAliveSeconds").addLongField("createTime")
				.addLongField("lastIncomingTime").build();
	}
}
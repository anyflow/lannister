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

package net.anyflow.lannister.session;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.mqtt.MqttMessage;
import net.anyflow.lannister.Literals;
import net.anyflow.lannister.Repository;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.serialization.ChannelIdSerializer;
import net.anyflow.lannister.serialization.Jsonizable;
import net.anyflow.lannister.serialization.SerializableFactory;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicMatcher;
import net.anyflow.lannister.topic.TopicSubscription;

public class Session extends Jsonizable implements com.hazelcast.nio.serialization.Portable {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);

	private static final int MAX_MESSAGE_ID_NUM = 0xffff;
	private static final int MIN_MESSAGE_ID_NUM = 1;

	public static Sessions NEXUS;
	public static final int ID = 4;

	@JsonProperty
	private String clientId;
	@JsonProperty
	private Map<String, TopicSubscription> topicSubscriptions;
	@JsonProperty
	private int currentMessageId;
	@JsonProperty
	private Message will;
	@JsonProperty
	private boolean cleanSession;
	@JsonProperty
	private int keepAliveSeconds;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	private Date createTime;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	private Date lastIncomingTime;

	private MessageSender messageSender;

	public Session() { // just for serialization
	}

	public Session(String clientId, int keepAliveSeconds, boolean cleanSession, Message will) {
		this.clientId = clientId;
		this.createTime = new Date();
		this.currentMessageId = 0;
		this.keepAliveSeconds = keepAliveSeconds;
		this.lastIncomingTime = new Date();
		this.cleanSession = cleanSession;
		this.will = will; // [MQTT-3.1.2-9]
		this.topicSubscriptions = Repository.SELF.generator().getMap(topicSubscriptionsName());

		this.messageSender = new MessageSender(this);
	}

	private String topicSubscriptionsName() {
		return "CLIENTID(" + clientId + ")_topicSubscriptions";
	}

	@JsonSerialize(using = ChannelIdSerializer.class)
	@JsonProperty
	public ChannelId channelId() {
		ChannelHandlerContext ctx = NEXUS.channelHandlerContext(clientId);
		if (ctx == null) { return null; }

		return ctx.channel().id();
	}

	@JsonProperty
	public boolean isConnected() {
		ChannelHandlerContext ctx = NEXUS.channelHandlerContext(clientId);
		if (ctx == null) { return false; }

		return ctx.channel().isActive();
	}

	public String clientId() {
		return clientId;
	}

	public Message will() {
		return will;
	}

	public boolean cleanSession() {
		return cleanSession;
	}

	public boolean isExpired() {
		if (keepAliveSeconds == 0) { return false; }

		return ((new Date()).getTime() - lastIncomingTime.getTime()) * 1000 < keepAliveSeconds;
	}

	public void setLastIncomingTime(Date lastIncomingTime) {
		this.lastIncomingTime = lastIncomingTime;

		Session.NEXUS.persist(this);
	}

	public ImmutableMap<String, TopicSubscription> topicSubscriptions() {
		return ImmutableMap.copyOf(topicSubscriptions);
	}

	public Stream<TopicSubscription> matches(String topicName) {
		return topicSubscriptions.values().stream().filter(t -> TopicMatcher.match(t.topicFilter(), topicName));
	}

	public TopicSubscription putTopicSubscription(TopicSubscription topicSubscription) {
		TopicSubscription ret = topicSubscriptions.put(topicSubscription.topicFilter(), topicSubscription); // [MQTT-3.8.4-3]

		Topic.NEXUS.map().values().stream().filter(t -> TopicMatcher.match(topicSubscription.topicFilter(), t.name()))
				.forEach(t -> t.addSubscriber(clientId));

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

	public void completeRemainedMessages() {
		messageSender.completeRemainedMessages();
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

		Session.NEXUS.persist(this);

		return currentMessageId;
	}

	public void dispose(boolean sendWill) {
		if (sendWill && will != null) { // [MQTT-3.1.2-10]
			Topic.NEXUS.get(will.topicName()).publish(clientId, will);
		}
		else {
			ChannelHandlerContext ctx = NEXUS.channelHandlerContext(clientId);

			ctx.disconnect().addListener(ChannelFutureListener.CLOSE);
			logger.debug("Session disposed [clientId={}/channelId={}]", clientId, ctx.channel().id());
		}

		NEXUS.remove(this);
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
		writer.writeBoolean("isCleanSession", cleanSession);
		writer.writeInt("keepAliveSeconds", keepAliveSeconds);
		writer.writeLong("createTime", createTime.getTime());
		writer.writeLong("lastIncomingTime", lastIncomingTime.getTime());
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		clientId = reader.readUTF("clientId");
		currentMessageId = reader.readInt("currentMessageId");
		will = reader.readPortable("will");
		cleanSession = reader.readBoolean("isCleanSession");
		keepAliveSeconds = reader.readInt("keepAliveSeconds");
		createTime = new Date(reader.readLong("createTime"));
		lastIncomingTime = new Date(reader.readLong("lastIncomingTime"));

		topicSubscriptions = Repository.SELF.generator().getMap(topicSubscriptionsName());

		messageSender = new MessageSender(this);
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("clientId")
				.addIntField("currentMessageId").addPortableField("will", Message.classDefinition())
				.addBooleanField("isCleanSession").addIntField("keepAliveSeconds").addLongField("createTime")
				.addLongField("lastIncomingTime").build();
	}
}
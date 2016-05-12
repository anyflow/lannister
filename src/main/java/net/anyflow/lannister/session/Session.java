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
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.mqtt.MqttMessage;
import net.anyflow.lannister.Hazelcast;
import net.anyflow.lannister.Literals;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.serialization.ChannelIdSerializer;
import net.anyflow.lannister.serialization.SerializableFactory;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicMatcher;
import net.anyflow.lannister.topic.TopicSubscription;

public class Session implements com.hazelcast.nio.serialization.Portable {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);

	public static Sessions NEXUS;
	public static final int ID = 4;

	@JsonProperty
	private String clientId;
	@JsonProperty
	private IMap<String, TopicSubscription> topicSubscriptions;
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
		this.topicSubscriptions = Hazelcast.SELF.generator().getMap(topicSubscriptionsName());
		this.topicSubscriptions.addInterceptor(new TopicSubscriptionInterceptor(clientId));

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

	public void will(Message will) {
		this.will = will;

		Session.NEXUS.persist(this);
	}

	public boolean cleanSession() {
		return cleanSession;
	}

	public boolean isExpired() {
		if (keepAliveSeconds == 0) { return false; }

		return ((new Date()).getTime() - lastIncomingTime.getTime()) > keepAliveSeconds * 1.5 * 1000;
	}

	public void setLastIncomingTime(Date lastIncomingTime) {
		this.lastIncomingTime = lastIncomingTime;

		Session.NEXUS.persist(this);
	}

	public IMap<String, TopicSubscription> topicSubscriptions() {
		return topicSubscriptions;
	}

	public Stream<TopicSubscription> matches(String topicName) {
		return topicSubscriptions.values().stream().filter(t -> TopicMatcher.match(t.topicFilter(), topicName));
	}

	public ChannelFuture send(MqttMessage message) {
		return messageSender.send(message);
	}

	public void sendPublish(Topic topic, Message message) {
		messageSender.sendPublish(topic, message);
	}

	public void completeRemainedMessages() {
		messageSender.completeRemainedMessages();
	}

	public Stream<Topic> topics(Collection<TopicSubscription> topicSubscriptions) {
		return Topic.NEXUS.map().values().parallelStream().filter(t -> this.matches(t.name()).count() > 0);
	}

	public int nextMessageId() {
		currentMessageId = currentMessageId + 1;

		if (currentMessageId > Message.MAX_MESSAGE_ID_NUM) {
			currentMessageId = Message.MIN_MESSAGE_ID_NUM;
		}

		Session.NEXUS.persist(this);

		return currentMessageId;
	}

	public void dispose(boolean sendWill) {
		if (sendWill && will != null) { // [MQTT-3.1.2-12]
			Topic.NEXUS.get(will.topicName()).publish(clientId, will);
			will(null); // [MQTT-3.1.2-10]
		}

		ChannelId channelId = null;
		ChannelHandlerContext ctx = NEXUS.channelHandlerContext(clientId);
		if (ctx != null) {
			ctx.disconnect().addListener(ChannelFutureListener.CLOSE);
			channelId = ctx.channel().id();
		}

		logger.debug("Session disposed [clientId={}/channelId={}]", clientId, ctx == null ? "null" : channelId);

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

		topicSubscriptions = Hazelcast.SELF.generator().getMap(topicSubscriptionsName());

		messageSender = new MessageSender(this);
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("clientId")
				.addIntField("currentMessageId").addPortableField("will", Message.classDefinition())
				.addBooleanField("isCleanSession").addIntField("keepAliveSeconds").addLongField("createTime")
				.addLongField("lastIncomingTime").build();
	}
}
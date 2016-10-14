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
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.anyflow.lannister.AbnormalDisconnectEventArgs;
import net.anyflow.lannister.Literals;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.plugin.DisconnectEventArgs;
import net.anyflow.lannister.plugin.DisconnectEventListener;
import net.anyflow.lannister.plugin.Plugins;
import net.anyflow.lannister.serialization.ChannelIdSerializer;
import net.anyflow.lannister.serialization.SerializableFactory;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicMatcher;
import net.anyflow.lannister.topic.TopicSubscriber;
import net.anyflow.lannister.topic.TopicSubscription;

public class Session implements com.hazelcast.nio.serialization.IdentifiedDataSerializable {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);

	public static final Sessions NEXUS = new Sessions();
	public static final int ID = 4;

	@JsonProperty
	private String clientId;
	@JsonProperty
	private String ip;
	@JsonProperty
	private int port;
	@JsonProperty
	private boolean isConnected;
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

	public Session(String clientId, String ip, int port, int keepAliveSeconds, boolean cleanSession, Message will) {
		this.clientId = clientId;
		this.ip = ip;
		this.port = port;
		this.isConnected = true;
		this.createTime = new Date();
		this.currentMessageId = 0;
		this.keepAliveSeconds = keepAliveSeconds;
		this.lastIncomingTime = new Date();
		this.cleanSession = cleanSession;
		this.will = will; // [MQTT-3.1.2-9]

		this.messageSender = new MessageSender(this);
	}

	@JsonSerialize(using = ChannelIdSerializer.class)
	@JsonProperty
	public ChannelId channelId() {
		ChannelHandlerContext ctx = NEXUS.channelHandlerContext(clientId);
		if (ctx == null) { return null; }

		return ctx.channel().id();
	}

	public boolean isConnected(boolean checkOwnership) {
		if (!isConnected) { return false; }
		if (!checkOwnership) { return isConnected; }

		ChannelHandlerContext ctx = NEXUS.channelHandlerContext(clientId);
		if (ctx == null) { return false; }

		return ctx.channel().isActive();
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;

		Session.NEXUS.persist(this);
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

		return (new Date().getTime() - lastIncomingTime.getTime()) > keepAliveSeconds * 1.5 * 1000;
	}

	public void setLastIncomingTime(Date lastIncomingTime) {
		this.lastIncomingTime = lastIncomingTime;

		Session.NEXUS.persist(this);
	}

	public TopicSubscription matches(String topicName) {
		return TopicSubscription.NEXUS.getTopicFiltersOf(clientId).stream()
				.filter(topicFilter -> TopicMatcher.match(topicFilter, topicName))
				.map(topicFilter -> TopicSubscription.NEXUS.getBy(topicFilter, clientId))
				.max((p1, p2) -> p1.qos().compareTo(p2.qos())).orElse(null); // [MQTT-3.3.5-1]
	}

	public void send(MqttMessage message, GenericFutureListener<? extends Future<? super Void>> completeListener) {
		messageSender.send(message, completeListener);
	}

	protected void sendPublish(Topic topic, Message message) {
		messageSender.sendPublish(topic, message);
	}

	public void completeRemainedMessages() {
		messageSender.completeRemainedMessages();
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
		setConnected(false);

		if (sendWill && will != null) { // [MQTT-3.1.2-12]
			Topic topic = Topic.NEXUS.prepare(will);
			topic.publish(will);

			will(null); // [MQTT-3.1.2-10]
		}

		ChannelId channelId = null;
		ChannelHandlerContext ctx = NEXUS.channelHandlerContext(clientId);
		if (ctx != null) {
			ctx.channel().disconnect().addListener(ChannelFutureListener.CLOSE).addListener(fs -> Plugins.INSTANCE
					.get(DisconnectEventListener.class).disconnected(new AbnormalDisconnectEventArgs()));

			channelId = ctx.channel().id();
		}

		logger.debug("Session disposed [clientId={}/channelId={}]", clientId, ctx == null ? "null" : channelId);

		if (cleanSession) {
			TopicSubscriber.NEXUS.removeByClientId(clientId);
			TopicSubscription.NEXUS.removeByClientId(clientId);
		}

		NEXUS.remove(this);

		Plugins.INSTANCE.get(DisconnectEventListener.class).disconnected(new DisconnectEventArgs() {
			@Override
			public String clientId() {
				return clientId;
			}

			@Override
			public Boolean cleanSession() {
				return cleanSession;
			}

			@Override
			public Boolean byDisconnectMessage() {
				return !sendWill;
			}
		});
	}

	@JsonIgnore
	@Override
	public int getFactoryId() {
		return SerializableFactory.ID;
	}

	@Override
	public int getId() {
		return ID;
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(clientId);
		out.writeUTF(ip);
		out.writeInt(port);
		out.writeBoolean(isConnected);
		out.writeInt(currentMessageId);

		out.writeBoolean(will != null);
		if (will != null) {
			will.writeData(out);
		}

		out.writeBoolean(cleanSession);
		out.writeInt(keepAliveSeconds);
		out.writeLong(createTime != null ? createTime.getTime() : Long.MIN_VALUE);
		out.writeLong(lastIncomingTime != null ? lastIncomingTime.getTime() : Long.MIN_VALUE);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		clientId = in.readUTF();
		ip = in.readUTF();
		port = in.readInt();
		isConnected = in.readBoolean();
		currentMessageId = in.readInt();

		if (in.readBoolean()) {
			will = new Message(in);
		}

		cleanSession = in.readBoolean();
		keepAliveSeconds = in.readInt();

		long rawLong = in.readLong();
		createTime = rawLong != Long.MIN_VALUE ? new Date(rawLong) : null;

		rawLong = in.readLong();
		lastIncomingTime = rawLong != Long.MIN_VALUE ? new Date(rawLong) : null;

		messageSender = new MessageSender(this);
	}
}
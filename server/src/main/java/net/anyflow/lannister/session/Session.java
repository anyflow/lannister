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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Lists;
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
import net.anyflow.lannister.AbnormalDisconnectEventArgs;
import net.anyflow.lannister.Hazelcast;
import net.anyflow.lannister.Literals;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.plugin.DisconnectEventArgs;
import net.anyflow.lannister.plugin.DisconnectEventListener;
import net.anyflow.lannister.plugin.Plugins;
import net.anyflow.lannister.serialization.ChannelIdSerializer;
import net.anyflow.lannister.serialization.SerializableFactory;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.TopicMatcher;
import net.anyflow.lannister.topic.TopicSubscription;

public class Session implements com.hazelcast.nio.serialization.Portable {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Session.class);

	public static final Sessions NEXUS = new Sessions();
	public static final int ID = 4;

	@JsonProperty
	private String clientId;
	@JsonProperty
	private String ip;
	@JsonProperty
	private Integer port;
	@JsonProperty
	private Boolean isConnected;
	@JsonProperty
	private IMap<String, TopicSubscription> topicSubscriptions;
	@JsonProperty
	private Integer currentMessageId;
	@JsonProperty
	private Message will;
	@JsonProperty
	private Boolean cleanSession;
	@JsonProperty
	private Integer keepAliveSeconds;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	private Date createTime;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	private Date lastIncomingTime;

	private MessageSender messageSender;

	public Session() { // just for serialization
	}

	public Session(String clientId, String ip, Integer port, int keepAliveSeconds, boolean cleanSession, Message will) {
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
		this.topicSubscriptions = Hazelcast.INSTANCE.getMap(topicSubscriptionsName());
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

	public IMap<String, TopicSubscription> topicSubscriptions() {
		return topicSubscriptions;
	}

	public TopicSubscription matches(String topicName) {
		return topicSubscriptions.values().stream().filter(t -> TopicMatcher.match(t.topicFilter(), topicName))
				.max((p1, p2) -> p1.qos().compareTo(p2.qos())).orElse(null); // [MQTT-3.3.5-1]
	}

	public ChannelFuture send(MqttMessage message) {
		return messageSender.send(message);
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
			this.topicSubscriptions.values().stream().forEach(ts -> {
				Topic.NEXUS.matches(ts.topicFilter()).forEach(t -> t.subscribers().remove(clientId));
			});

			this.topicSubscriptions.destroy();
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

	@JsonIgnore
	@Override
	public int getClassId() {
		return ID;
	}

	@Override
	public void writePortable(PortableWriter writer) throws IOException {
		List<String> nullChecker = Lists.newArrayList();

		if (clientId != null) {
			writer.writeUTF("clientId", clientId);
			nullChecker.add("clientId");
		}

		if (ip != null) {
			writer.writeUTF("ip", ip);
			nullChecker.add("ip");
		}

		if (port != null) {
			writer.writeInt("port", port);
			nullChecker.add("port");
		}

		if (isConnected != null) {
			writer.writeBoolean("isConnected", isConnected);
			nullChecker.add("isConnected");
		}

		if (currentMessageId != null) {
			writer.writeInt("currentMessageId", currentMessageId);
			nullChecker.add("currentMessageId");
		}

		if (will != null) {
			writer.writePortable("will", will);
		}
		else {
			writer.writeNullPortable("will", SerializableFactory.ID, Message.ID);
		}

		if (cleanSession != null) {
			writer.writeBoolean("cleanSession", cleanSession);
			nullChecker.add("cleanSession");
		}

		if (keepAliveSeconds != null) {
			writer.writeInt("keepAliveSeconds", keepAliveSeconds);
			nullChecker.add("keepAliveSeconds");
		}

		if (createTime != null) {
			writer.writeLong("createTime", createTime.getTime());
			nullChecker.add("createTime");
		}

		if (lastIncomingTime != null) {
			writer.writeLong("lastIncomingTime", lastIncomingTime.getTime());
			nullChecker.add("lastIncomingTime");
		}

		writer.writeUTFArray("nullChecker", nullChecker.toArray(new String[0]));
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		List<String> nullChecker = Lists.newArrayList(reader.readUTFArray("nullChecker"));

		if (nullChecker.contains("clientId")) clientId = reader.readUTF("clientId");
		if (nullChecker.contains("ip")) ip = reader.readUTF("ip");
		if (nullChecker.contains("port")) port = reader.readInt("port");
		if (nullChecker.contains("isConnected")) isConnected = reader.readBoolean("isConnected");
		if (nullChecker.contains("currentMessageId")) currentMessageId = reader.readInt("currentMessageId");
		will = reader.readPortable("will");
		if (nullChecker.contains("cleanSession")) cleanSession = reader.readBoolean("cleanSession");
		if (nullChecker.contains("keepAliveSeconds")) keepAliveSeconds = reader.readInt("keepAliveSeconds");
		if (nullChecker.contains("createTime")) createTime = new Date(reader.readLong("createTime"));
		if (nullChecker.contains("lastIncomingTime")) lastIncomingTime = new Date(reader.readLong("lastIncomingTime"));

		topicSubscriptions = Hazelcast.INSTANCE.getMap(topicSubscriptionsName());

		messageSender = new MessageSender(this);
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("clientId").addUTFField("ip")
				.addIntField("port").addBooleanField("isConnected").addIntField("currentMessageId")
				.addPortableField("will", Message.classDefinition()).addBooleanField("cleanSession")
				.addIntField("keepAliveSeconds").addLongField("createTime").addLongField("lastIncomingTime")
				.addUTFArrayField("nullChecker").build();
	}
}
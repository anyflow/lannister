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

package net.anyflow.lannister.topic;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.Repository;
import net.anyflow.lannister.message.InboundMessageStatus;
import net.anyflow.lannister.message.InboundMessageStatus.Status;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.MessageStatus;
import net.anyflow.lannister.serialization.Jsonizable;
import net.anyflow.lannister.serialization.SerializableFactory;
import net.anyflow.lannister.session.Session;

public class Topic extends Jsonizable implements com.hazelcast.nio.serialization.Portable {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Topic.class);

	public static Topics NEXUS;
	public static final int ID = 6;

	@JsonProperty
	private String name;
	@JsonProperty
	private Message retainedMessage;
	@JsonProperty
	private IMap<String, TopicSubscriber> subscribers; // clientIds
	@JsonProperty
	private IMap<String, Message> messages; // KEY:Message.key()
	@JsonProperty
	private IMap<String, InboundMessageStatus> inboundMessageStatuses; // KEY:clientId_messageId

	public Topic() { // just for Serialization
	}

	public Topic(String name) {
		this.name = name;
		this.retainedMessage = null;
		this.subscribers = Repository.SELF.generator().getMap(subscribersName());
		this.messages = Repository.SELF.generator().getMap(messagesName());
		this.inboundMessageStatuses = Repository.SELF.generator().getMap(inboundMessageStatusesName());
	}

	private String subscribersName() {
		return "TOPIC(" + name + ")_subscribers";
	}

	private String messagesName() {
		return "TOPIC(" + name + ")_messages";
	}

	private String inboundMessageStatusesName() {
		return "TOPIC(" + name + ")_inboundMessageStatuses";
	}

	public String name() {
		return name;
	}

	public Message retainedMessage() {
		return retainedMessage;
	}

	public void setRetainedMessage(Message message) {
		this.retainedMessage = message;
		NEXUS.put(this);
	}

	public ImmutableMap<String, TopicSubscriber> subscribers() {
		return ImmutableMap.copyOf(subscribers);
	}

	public IMap<String, Message> messages() {
		return messages;
	}

	public ImmutableMap<String, InboundMessageStatus> inboundMessageStatuses() {
		return ImmutableMap.copyOf(inboundMessageStatuses);
	}

	public InboundMessageStatus getInboundMessageStatus(String clientId, int messageId) {
		return inboundMessageStatuses.get(MessageStatus.key(clientId, messageId));
	}

	public void removeInboundMessageStatus(String clientId, int messageId) {
		inboundMessageStatuses.remove(MessageStatus.key(clientId, messageId));
	}

	public void setInboundMessageStatus(String clientId, int messageId, Status targetStatus) {
		InboundMessageStatus status = inboundMessageStatuses.get(MessageStatus.key(clientId, messageId));
		if (status == null) {
			status = new InboundMessageStatus(clientId, messageId);
		}
		status.status(targetStatus);

		inboundMessageStatuses.put(status.key(), status);
	}

	public void addSubscriber(String clientId) {
		subscribers.put(clientId, new TopicSubscriber(clientId, name));
	}

	public void removeSubscriber(String clientId) {
		subscribers.remove(clientId);

		// TODO should be this topic remained in spite of no subscriber?
	}

	public void putMessage(String requesterId, Message message) {
		if (message.qos() == MqttQoS.AT_MOST_ONCE) { return; }

		messages.put(message.key(), message);

		setInboundMessageStatus(requesterId, message.id(), Status.RECEIVED);
	}

	public void broadcast(Message message) {
		subscribers.keySet().stream().parallel().forEach(id -> {
			Session session = Session.NEXUS.map().values().stream()
					.filter(s -> id.equals(s.clientId()) && s.isConnected()).findFirst().orElse(null);

			if (session != null) {
				session.sendPublish(this, message, false);
			}
			else {
				NEXUS.notifier().publish(new Notification(id, this, message));
			}
		});
	}

	public void publish(String requesterId, Message message) {
		putMessage(requesterId, message);
		broadcast(message);
	}

	public static Topic put(Topic topic) {
		Session.NEXUS.topicAdded(topic);

		// TODO should be added in case of no subscriber & no retained Message?
		return NEXUS.put(topic);
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
		writer.writeUTF("name", name);

		if (retainedMessage != null) {
			writer.writePortable("retainedMessage", retainedMessage);
		}
		else {
			writer.writeNullPortable("retainedMessage", SerializableFactory.ID, Message.ID);
		}
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		name = reader.readUTF("name");
		retainedMessage = reader.readPortable("retainedMessage");

		subscribers = Repository.SELF.generator().getMap(subscribersName());
		messages = Repository.SELF.generator().getMap(messagesName());
		inboundMessageStatuses = Repository.SELF.generator().getMap(inboundMessageStatusesName());
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("name")
				.addPortableField("retainedMessage", Message.classDefinition()).build();
	}
}
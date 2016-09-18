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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.Hazelcast;
import net.anyflow.lannister.message.InboundMessageStatus;
import net.anyflow.lannister.message.InboundMessageStatus.Status;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.serialization.SerializableFactory;
import net.anyflow.lannister.session.Session;

public class Topic implements com.hazelcast.nio.serialization.Portable {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Topic.class);

	public static final Topics NEXUS = new Topics(Session.NEXUS);
	public static final int ID = 6;

	@JsonProperty
	private String name;
	@JsonProperty
	private Message retainedMessage; // [MQTT-3.1.2.7]
	@JsonProperty
	private IMap<String, TopicSubscriber> subscribers; // clientIds
	@JsonProperty
	private IMap<String, Message> messages; // KEY:Message.key()
	@JsonProperty
	private IMap<String, InboundMessageStatus> inboundMessageStatuses; // KEY:Message.key()
	@JsonProperty
	private IMap<String, Integer> messageReferenceCounts; // KEY:Message.key()

	@JsonIgnore
	private ILock messageReferenceCountsLock;

	public Topic() { // just for Serialization
	}

	public Topic(String name) {
		this.name = name;
		this.retainedMessage = null;
		this.subscribers = Hazelcast.INSTANCE.getMap(subscribersName());
		this.subscribers.addInterceptor(new TopicSubscriberInterceptor(name));

		this.messages = Hazelcast.INSTANCE.getMap(messagesName());
		this.inboundMessageStatuses = Hazelcast.INSTANCE.getMap(inboundMessageStatusesName());
		this.messageReferenceCounts = Hazelcast.INSTANCE.getMap(inboundMessageReferenceCountsName());

		this.messageReferenceCountsLock = Hazelcast.INSTANCE.getLock(messageReferenceCountsLockName());
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

	private String inboundMessageReferenceCountsName() {
		return "TOPIC(" + name + ")_inboundMessageReferenceCounts";
	}

	private String messageReferenceCountsLockName() {
		return "TOPIC(" + name + ")_messageReferneceCount_Lock";
	}

	public String name() {
		return name;
	}

	public Message retainedMessage() {
		return retainedMessage;
	}

	public void setRetainedMessage(Message message) {
		if (message == null || message.message().length <= 0) {
			this.retainedMessage = null;
		}
		else {
			this.retainedMessage = message;
		}

		NEXUS.persist(this);
	}

	public IMap<String, TopicSubscriber> subscribers() {
		return subscribers;
	}

	public IMap<String, Message> messages() {
		return messages;
	}

	public IMap<String, InboundMessageStatus> inboundMessageStatuses() {
		return inboundMessageStatuses;
	}

	public InboundMessageStatus getInboundMessageStatus(String clientId, int messageId) {
		return inboundMessageStatuses.get(Message.key(clientId, messageId));
	}

	public void removeInboundMessageStatus(String clientId, int messageId) {
		String messageKey = Message.key(clientId, messageId);
		inboundMessageStatuses.remove(messageKey);
		release(messageKey);
	}

	public void addInboundMessageStatus(String clientId, int messageId, Status status) {
		InboundMessageStatus messageStatus = new InboundMessageStatus(clientId, messageId, status);
		retain(messageStatus.key());

		inboundMessageStatuses.put(messageStatus.key(), messageStatus);
	}

	public void setInboundMessageStatus(String clientId, int messageId, Status status) {
		InboundMessageStatus messageStatus = inboundMessageStatuses.get(Message.key(clientId, messageId));
		if (status == null) {
			logger.error("Inbound message status does not exist [clientId={}, messageId={}, status={}", clientId,
					messageId, null);
			throw new IllegalArgumentException();
		}

		messageStatus.status(status);

		inboundMessageStatuses.put(messageStatus.key(), messageStatus);
	}

	public void putMessage(String requesterId, Message message) {
		assert name.equals(message.topicName());

		if (message.qos() == MqttQoS.AT_MOST_ONCE) { return; }

		messages.put(message.key(), message);

		addInboundMessageStatus(requesterId, message.id(), Status.RECEIVED);
	}

	private static MqttQoS adjustQoS(MqttQoS subscriptionQos, MqttQoS publishQos) {
		return subscriptionQos.value() <= publishQos.value() ? subscriptionQos : publishQos;
	}

	public void publish(Session session, Message message) {
		assert session != null;
		assert message != null;

		message.setId(session.nextMessageId()); // [MQTT-2.3.1-2]

		if (message.qos() != MqttQoS.AT_MOST_ONCE) {
			Topic.NEXUS.get(message.topicName()).subscribers().get(session.clientId()).addOutboundMessageStatus(
					message.id(), message.key(), OutboundMessageStatus.Status.TO_PUBLISH, message.qos()); // [MQTT-3.1.2-5]
		}

		NEXUS.notifier().publish(new Notification(session.clientId(), this, message));
	}

	protected void publish(Message message) {
		assert name.equals(message.topicName());

		if (message.isRetain()) {// else do nothing [MQTT-3.3.1-12]
			setRetainedMessage(message); // [MQTT-3.3.1-5],[MQTT-3.3.1-10],[MQTT-3.3.1-11]
		}

		putMessage(message.publisherId(), message);

		subscribers.keySet().stream().filter(id -> Session.NEXUS.get(id) != null).forEach(id -> {
			Session session = Session.NEXUS.get(id);

			Message toSend = message.clone();

			TopicSubscription subscription = session.matches(name);
			assert subscription != null;

			toSend.setQos(adjustQoS(subscription.qos(), message.qos()));

			publish(session, toSend);
		});
	}

	public void addSubscribers() {
		Session.NEXUS.map().values().stream()
				.filter(s -> s.topicSubscriptions().values().stream()
						.anyMatch(ts -> TopicMatcher.match(ts.topicFilter(), name)))
				.forEach(s -> subscribers.put(s.clientId(), new TopicSubscriber(s.clientId(), name)));
	}

	public void retain(String messageKey) {
		messageReferenceCountsLock.lock();

		try {
			Integer count = messageReferenceCounts.get(messageKey);
			if (count == null) {
				count = 0;
			}

			messageReferenceCounts.put(messageKey, ++count);
			logger.debug("message reference added [count={}, messageKey={}]", count, messageKey);
		}
		finally {
			messageReferenceCountsLock.unlock();
		}
	}

	public void release(String messageKey) {
		messageReferenceCountsLock.lock();

		try {
			Integer count = messageReferenceCounts.get(messageKey);
			if (count <= 0) {
				logger.error("Message reference count error [key={}, count={}]", messageKey, count);
				return;
			}
			else if (count == 1) {
				messageReferenceCounts.remove(messageKey);
				messages.remove(messageKey);
				logger.debug("message removed [messageKey={}]", messageKey);
			}
			else {
				messageReferenceCounts.put(messageKey, --count);
				logger.debug("message rereference released [count={}, messageKey={}]", count, messageKey);
			}
		}
		finally {
			messageReferenceCountsLock.unlock();
		}
	}

	public void dispose() {
		subscribers.destroy();
		messages.destroy();
		inboundMessageStatuses.destroy();
		messageReferenceCounts.destroy();
		messageReferenceCountsLock.destroy();
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

		if (name != null) {
			writer.writeUTF("name", name);
			nullChecker.add("name");
		}

		if (retainedMessage != null) {
			writer.writePortable("retainedMessage", retainedMessage);
		}
		else {
			writer.writeNullPortable("retainedMessage", SerializableFactory.ID, Message.ID);
		}

		writer.writeUTFArray("nullChecker", nullChecker.toArray(new String[0]));
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		List<String> nullChecker = Lists.newArrayList(reader.readUTFArray("nullChecker"));

		if (nullChecker.contains("name")) name = reader.readUTF("name");

		retainedMessage = reader.readPortable("retainedMessage");

		subscribers = Hazelcast.INSTANCE.getMap(subscribersName());
		messages = Hazelcast.INSTANCE.getMap(messagesName());
		inboundMessageStatuses = Hazelcast.INSTANCE.getMap(inboundMessageStatusesName());
		messageReferenceCounts = Hazelcast.INSTANCE.getMap(inboundMessageReferenceCountsName());
		messageReferenceCountsLock = Hazelcast.INSTANCE.getLock(messageReferenceCountsLockName());
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("name")
				.addPortableField("retainedMessage", Message.classDefinition()).addUTFArrayField("nullChecker").build();
	}
}
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
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.serialization.SerializableFactory;
import net.anyflow.lannister.session.Session;

public class Topic implements com.hazelcast.nio.serialization.IdentifiedDataSerializable {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Topic.class);

	public static final Topics NEXUS = new Topics(Session.NEXUS);
	public static final int ID = 6;

	@JsonProperty
	private String name;
	@JsonProperty
	private Message retainedMessage; // [MQTT-3.1.2.7]

	public Topic() { // just for Serialization
	}

	public Topic(String name) {
		this.name = name;
		this.retainedMessage = null;
	}

	public String name() {
		return name;
	}

	public Message retainedMessage() {
		return retainedMessage;
	}

	public void setRetainedMessage(Message message) {
		if (message == null || message.message().length <= 0) { // [mqtt-3.3.1-10],[MQTT-3.3.1-11]
			this.retainedMessage = null;
		}
		else {
			this.retainedMessage = message.clone();
		}

		NEXUS.persist(this);
	}

	private static MqttQoS adjustQoS(MqttQoS subscriptionQos, MqttQoS publishQos) {
		return subscriptionQos.value() <= publishQos.value() ? subscriptionQos : publishQos;
	}

	public void publish(final Message message) {
		assert message != null;
		assert name.equals(message.topicName());

		if (message.qos() != MqttQoS.AT_MOST_ONCE) {
			Message.NEXUS.put(message.key(), message);
		}

		TopicSubscriber.NEXUS.clientIdsOf(name).forEach(id -> {
			Session session = Session.NEXUS.get(id);
			logger.debug("Before publish to [clientId={}, sessionsSize={}]", id, Session.NEXUS.keySet().size());
			assert session != null;

			publish(session, message);
		});
	}

	public void publish(final Session session, final Message message) {
		assert session != null;
		assert message != null;

		Message toSend = message.clone();

		TopicSubscription subscription = session.matches(name);
		assert subscription != null;

		toSend.qos(adjustQoS(subscription.qos(), toSend.qos()));

		if (!OutboundMessageStatus.NEXUS.containsKey(toSend.id(), session.clientId())) {
			toSend.id(session.nextMessageId()); // [MQTT-2.3.1-2]

			if (toSend.qos() != MqttQoS.AT_MOST_ONCE) {
				OutboundMessageStatus outboundMessageStatus = new OutboundMessageStatus(message.key(),
						session.clientId(), toSend.id(), toSend.topicName(), OutboundMessageStatus.Status.TO_PUBLISH,
						toSend.qos()); // [MQTT-3.1.2-5]

				OutboundMessageStatus.NEXUS.put(outboundMessageStatus);
			}
		}

		NEXUS.notifier().publish(new Notification(session.clientId(), this, toSend));
	}

	public void publish(Session session, String messageKey) {
		assert session != null;
		assert messageKey != null;

		Message message = Message.NEXUS.get(messageKey);
		assert message != null;
		assert OutboundMessageStatus.NEXUS.containsKey(message.id(), session.clientId());

		NEXUS.notifier().publish(new Notification(session.clientId(), this, message));
	}

	@JsonIgnore
	@Override
	public int getFactoryId() {
		return SerializableFactory.ID;
	}

	@JsonIgnore
	@Override
	public int getId() {
		return ID;
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(name);

		out.writeBoolean(retainedMessage != null);
		if (retainedMessage != null) {
			retainedMessage.writeData(out);
		}
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		name = in.readUTF();

		if (in.readBoolean()) {
			retainedMessage = new Message(in);
		}
	}
}
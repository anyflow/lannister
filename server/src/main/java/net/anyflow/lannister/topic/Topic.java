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
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.Hazelcast;
import net.anyflow.lannister.message.InboundMessageStatus;
import net.anyflow.lannister.message.InboundMessageStatus.Status;
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
    @JsonProperty
    private IMap<String, TopicSubscriber> subscribers; // KEY:clientId
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
        return "TOPIC(" + name + ")_messageReferenceCount_Lock";
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

        inboundMessageStatuses.set(messageStatus.key(), messageStatus);
    }

    public void setInboundMessageStatus(String clientId, int messageId, Status status) {
        InboundMessageStatus messageStatus = inboundMessageStatuses.get(Message.key(clientId, messageId));
        if (status == null) {
            logger.error("Inbound message status does not exist [clientId={}, messageId={}, status={}", clientId,
                    messageId, null);
            throw new IllegalArgumentException();
        }

        messageStatus.status(status);

        inboundMessageStatuses.set(messageStatus.key(), messageStatus);
    }

    private void putMessage(String requesterId, Message message) {
        assert name.equals(message.topicName());

        if (message.qos() == MqttQoS.AT_MOST_ONCE) { return; }

        messages.set(message.key(), message);

        addInboundMessageStatus(requesterId, message.id(), Status.RECEIVED);
    }

    private static MqttQoS adjustQoS(MqttQoS subscriptionQos, MqttQoS publishQos) {
        return subscriptionQos.value() <= publishQos.value() ? subscriptionQos : publishQos;
    }

    public void publish(final Message message) {
        assert message != null;
        assert name.equals(message.topicName());

        putMessage(message.publisherId(), message);

        subscribers.keySet().stream().forEach(id -> {
            Session session = Session.NEXUS.get(id);
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

        toSend.setQos(adjustQoS(subscription.qos(), toSend.qos()));

        TopicSubscriber ts = subscribers().get(session.clientId());
        assert ts != null;

        if (!ts.outboundMessageStatuses().containsKey(toSend.id())) {
            String messageKey = toSend.key();

            toSend.setId(session.nextMessageId()); // [MQTT-2.3.1-2]

            if (toSend.qos() != MqttQoS.AT_MOST_ONCE) {
                ts.addOutboundMessageStatus(messageKey, toSend.id(), OutboundMessageStatus.Status.TO_PUBLISH,
                        toSend.qos()); // [MQTT-3.1.2-5]
            }
        }

        NEXUS.notifier().publish(new Notification(session.clientId(), this, toSend));
    }

    public void publish(Session session, String messageKey) {
        assert session != null;
        assert messageKey != null;

        Message message = messages.get(messageKey);
        assert message != null;
        assert subscribers().get(session.clientId()).outboundMessageStatuses().containsKey(message.id());

        NEXUS.notifier().publish(new Notification(session.clientId(), this, message));
    }

    public void updateSubscribers() {
        Session.NEXUS.map().values().stream()
                .filter(s -> s.topicSubscriptions().values().stream()
                        .anyMatch(ts -> TopicMatcher.match(ts.topicFilter(), name)))
                .forEach(s -> subscribers.set(s.clientId(), new TopicSubscriber(s.clientId(), name)));
    }

    public void retain(String messageKey) {
        messageReferenceCountsLock.lock();

        try {
            Integer count = messageReferenceCounts.get(messageKey);
            if (count == null) {
                count = 0;
            }

            messageReferenceCounts.set(messageKey, ++count);
            logger.debug("message reference added [count={}, messageKey={}]", count, messageKey);
        }
        finally {
            messageReferenceCountsLock.unlock();
        }
    }

    public void release(String messageKey) {
        Integer count = messageReferenceCounts.get(messageKey);
        if (count == null) { return; }

        messageReferenceCountsLock.lock();

        try {
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
                logger.debug("message reference released [count={}, messageKey={}]", count, messageKey);
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
            Message temp = new Message();
            temp.readData(in);

            retainedMessage = temp;
        }

        subscribers = Hazelcast.INSTANCE.getMap(subscribersName());
        messages = Hazelcast.INSTANCE.getMap(messagesName());
        inboundMessageStatuses = Hazelcast.INSTANCE.getMap(inboundMessageStatusesName());
        messageReferenceCounts = Hazelcast.INSTANCE.getMap(inboundMessageReferenceCountsName());
        messageReferenceCountsLock = Hazelcast.INSTANCE.getLock(messageReferenceCountsLockName());
    }
}
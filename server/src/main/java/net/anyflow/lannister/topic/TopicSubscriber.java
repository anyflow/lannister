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
import com.hazelcast.core.IMap;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.Hazelcast;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.serialization.SerializableFactory;

public class TopicSubscriber implements com.hazelcast.nio.serialization.IdentifiedDataSerializable {

    public final static int ID = 7;

    @JsonProperty
    private String clientId;
    @JsonProperty
    private String topicName;
    @JsonProperty
    private IMap<Integer, OutboundMessageStatus> outboundMessageStatuses; // KEY:messageId

    public TopicSubscriber() { // just for Serialization
    }

    public TopicSubscriber(String clientId, String topicName) {
        this.clientId = clientId;
        this.topicName = topicName;
        this.outboundMessageStatuses = Hazelcast.INSTANCE.getMap(messageStatusesName());
    }

    private String messageStatusesName() {
        return "TOPIC(" + topicName + ")_CLIENT(" + clientId + ")_outboundMessageStatuses";
    }

    public IMap<Integer, OutboundMessageStatus> outboundMessageStatuses() {
        return outboundMessageStatuses;
    }

    protected void addOutboundMessageStatus(String messageKey, int messageId, OutboundMessageStatus.Status status,
            MqttQoS qos) {
        OutboundMessageStatus messageStatus = new OutboundMessageStatus(messageKey, clientId, messageId, status, qos);

        outboundMessageStatuses.set(messageStatus.messageId(), messageStatus);

        Topic.NEXUS.get(topicName).retain(messageStatus.messageKey());
    }

    public OutboundMessageStatus removeOutboundMessageStatus(int messageId) {
        OutboundMessageStatus ret = outboundMessageStatuses.remove(messageId);
        if (ret == null) { return null; }

        Topic.NEXUS.get(topicName).release(ret.messageKey());

        return ret;
    }

    public void setOutboundMessageStatus(int messageId, OutboundMessageStatus.Status targetStatus) {
        OutboundMessageStatus status = outboundMessageStatuses.get(messageId);
        if (status == null) { return; }

        status.status(targetStatus);

        outboundMessageStatuses.set(status.messageId(), status);
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
        out.writeUTF(topicName);
        out.writeUTF(clientId);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        topicName = in.readUTF();
        clientId = in.readUTF();

        outboundMessageStatuses = Hazelcast.INSTANCE.getMap(messageStatusesName());
    }
}
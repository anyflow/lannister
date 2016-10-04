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
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.serialization.SerializableFactory;

public class Notification implements com.hazelcast.nio.serialization.IdentifiedDataSerializable {
    public final static int ID = 5;

    private String clientId;
    private Topic topic;
    private Message message;

    public Notification() { // just for Serialization
    }

    protected Notification(String clientId, Topic topic, Message message) {
        this.clientId = clientId;
        this.topic = topic;
        this.message = message;
    }

    public String clientId() {
        return clientId;
    }

    public Topic topic() {
        return topic;
    }

    public Message message() {
        return message;
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
        out.writeBoolean(topic != null);
        if (topic != null) {
            topic.writeData(out);
        }
        out.writeBoolean(message != null);
        if (message != null) {
            message.writeData(out);
        }
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        clientId = in.readUTF();
        if (in.readBoolean()) {
            Topic temp = new Topic();
            temp.readData(in);

            topic = temp;
        }
        if (in.readBoolean()) {
            Message temp = new Message();
            temp.readData(in);

            message = temp;
        }
    }
}
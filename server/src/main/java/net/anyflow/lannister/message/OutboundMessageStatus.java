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

package net.anyflow.lannister.message;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.serialization.SerializableFactory;

public class OutboundMessageStatus extends MessageStatus {
    public static final int ID = 3;

    @JsonProperty
    private String messageKey;
    @JsonProperty
    private Status status;
    @JsonProperty
    private MqttQoS qos;

    public OutboundMessageStatus() { // just for Serialization
    }

    public OutboundMessageStatus(String messageKey, String clientId, int messageId, Status status, MqttQoS qos) {
        super(clientId, messageId);

        this.messageKey = messageKey;
        this.status = status;
        this.qos = qos;
    }

    public String messageKey() {
        return messageKey;
    }

    public Status status() {
        return status;
    }

    public void status(Status status) {
        this.status = status;
        super.updateTime = new Date();
    }

    public MqttQoS qos() {
        return qos;
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
        super.writeData(out);

        out.writeUTF(messageKey);

        if (status != null) {
            out.writeByte(status.value());
        }
        else {
            out.writeByte(Byte.MIN_VALUE);
        }

        if (qos != null) {
            out.writeInt(qos.value());
        }
        else {
            out.writeInt(Byte.MIN_VALUE);
        }
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        super.readData(in);

        messageKey = in.readUTF();
        
        Byte rawByte = in.readByte();
        if(rawByte != Byte.MIN_VALUE) {
            status = Status.valueOf(rawByte);    
        }
        else {
            status = null;
        }
        
        int rawInt = in.readInt();
        if(rawInt != Byte.MIN_VALUE) {
            qos = MqttQoS.valueOf(rawInt);    
        }
        else {
            qos = null;
        }
    }

    public enum Status {
        TO_PUBLISH((byte) 0),
        PUBLISHED((byte) 1),
        PUBRECED((byte) 2);

        private byte value;

        private Status(byte value) {
            this.value = value;
        }

        public byte value() {
            return value;
        }

        public static Status valueOf(byte value) {
            for (Status q : values()) {
                if (q.value == value) { return q; }
            }
            throw new IllegalArgumentException("Invalid SenderTargetStatus: " + value);
        }
    }
}
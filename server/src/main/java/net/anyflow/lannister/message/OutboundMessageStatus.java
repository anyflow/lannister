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
	public static final OutboundMessageStatuses NEXUS = new OutboundMessageStatuses();
	public static final int ID = 3;

	@JsonProperty
	private Status status;
	@JsonProperty
	private MqttQoS qos;

	public OutboundMessageStatus() { // just for Serialization
	}

	public OutboundMessageStatus(String messageKey, String clientId, int messageId, String topicName, Status status,
			MqttQoS qos) {
		super(messageKey, clientId, messageId, topicName);

		this.status = status;
		this.qos = qos;
	}

	@Override
	public String key() {
		return OutboundMessageStatuses.key(messageId(), clientId());
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

	@JsonIgnore
	@Override
	public int getId() {
		return ID;
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		super.writeData(out);

		out.writeByte(status != null ? status.value() : Byte.MIN_VALUE);
		out.writeInt(qos != null ? qos.value() : Byte.MIN_VALUE);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		super.readData(in);

		Byte rawByte = in.readByte();
		status = rawByte != Byte.MIN_VALUE ? Status.valueOf(rawByte) : null;

		int rawInt = in.readInt();
		qos = rawInt != Byte.MIN_VALUE ? MqttQoS.valueOf(rawInt) : null;
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
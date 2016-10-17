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

import net.anyflow.lannister.serialization.SerializableFactory;

public class InboundMessageStatus extends MessageStatus {
	public static final int ID = 2;
	public static final InboundMessageStatuses NEXUS = new InboundMessageStatuses();

	@JsonProperty
	private Status status;

	public InboundMessageStatus() { // just for Serialization
	}

	public InboundMessageStatus(String messageKey, String clientId, int messageId, String topicName, Status status) {
		super(messageKey, clientId, messageId, topicName);

		this.status = status;
	}

	@Override
	public String key() {
		return InboundMessageStatuses.key(messageId(), clientId());
	}

	public Status status() {
		return status;
	}

	public void status(Status status) {
		this.status = status;
		super.updateTime = new Date();
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

		out.writeByte(status != null ? status.value() : Byte.MIN_VALUE);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		super.readData(in);

		byte rawByte = in.readByte();
		status = rawByte != Byte.MIN_VALUE ? Status.valueOf(rawByte) : null;
	}

	public enum Status {
		RECEIVED((byte) 0),
		PUBRECED((byte) 1);

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
			throw new IllegalArgumentException("Invalid ReceiverTargetStatus: " + value);
		}
	}
}
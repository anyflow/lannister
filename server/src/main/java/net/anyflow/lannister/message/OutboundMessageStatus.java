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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

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

	@JsonIgnore
	@Override
	public int getClassId() {
		return ID;
	}

	@Override
	public void writePortable(PortableWriter writer) throws IOException {
		List<String> nullChecker = Lists.newArrayList();

		writePortable(nullChecker, writer);

		if (messageKey != null) {
			writer.writeUTF("messageKey", messageKey);
			nullChecker.add("messageKey");
		}
		if (status != null) {
			writer.writeByte("status", status.value());
			nullChecker.add("status");
		}
		if (qos != null) {
			writer.writeInt("qos", qos.value());
			nullChecker.add("qos");
		}

		writer.writeUTFArray("nullChecker", nullChecker.toArray(new String[0]));
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		List<String> nullChecker = Lists.newArrayList(reader.readUTFArray("nullChecker"));

		readPortable(nullChecker, reader);

		if (nullChecker.contains("messageKey")) messageKey = reader.readUTF("messageKey");
		if (nullChecker.contains("status")) status = Status.valueOf(reader.readByte("status"));
		if (nullChecker.contains("qos")) qos = MqttQoS.valueOf(reader.readInt("qos"));
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("clientId").addIntField("messageId")
				.addLongField("createTime").addLongField("updateTime").addUTFField("messageKey").addByteField("status")
				.addIntField("qos").addUTFArrayField("nullChecker").build();
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
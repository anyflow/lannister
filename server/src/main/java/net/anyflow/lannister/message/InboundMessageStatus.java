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

import net.anyflow.lannister.serialization.SerializableFactory;

public class InboundMessageStatus extends MessageStatus {

	public static final int ID = 2;

	@JsonProperty
	private Status status;

	public InboundMessageStatus() { // just for Serialization
	}

	public InboundMessageStatus(String clientId, int messageId, Status status) {
		super(clientId, messageId);

		this.status = status;
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

	@JsonIgnore
	@Override
	public int getClassId() {
		return ID;
	}

	@Override
	public void writePortable(PortableWriter writer) throws IOException {
		List<String> nullChecker = Lists.newArrayList();

		writePortable(nullChecker, writer);

		if (status != null) {
			writer.writeByte("targetStatus", status.value());
			nullChecker.add("id");
		}

		writer.writeUTFArray("nullChecker", nullChecker.toArray(new String[0]));
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		List<String> nullChecker = Lists.newArrayList(reader.readUTFArray("nullChecker"));

		readPortable(nullChecker, reader);

		if (nullChecker.contains("status")) status = Status.valueOf(reader.readByte("targetStatus"));
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("clientId").addIntField("messageId")
				.addByteField("targetStatus").addLongField("createTime").addLongField("updateTime")
				.addUTFArrayField("nullChecker").build();
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
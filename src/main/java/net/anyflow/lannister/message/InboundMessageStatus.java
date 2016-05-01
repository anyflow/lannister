package net.anyflow.lannister.message;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import net.anyflow.lannister.serialization.SerializableFactory;

public class InboundMessageStatus extends MessageStatus {

	public static final int ID = 2;

	@JsonProperty
	private Status targetStatus;

	public InboundMessageStatus() { // just for Serialization
	}

	public InboundMessageStatus(String clientId, int messageId) {
		super(clientId, messageId);

		targetStatus = Status.TO_ACK;
	}

	public Status targetStatus() {
		return targetStatus;
	}

	public void targetStatus(Status targetStatus) {
		this.targetStatus = targetStatus;
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
		super.writePortable(writer);

		writer.writeByte("targetStatus", targetStatus.value());
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		super.readPortable(reader);

		targetStatus = Status.valueOf(reader.readByte("targetStatus"));
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("clientId").addIntField("messageId")
				.addByteField("targetStatus").build();
	}

	public enum Status {
		TO_ACK((byte) 0), // RECEIVER acknowlegement
		TO_PUBREC((byte) 1), // RECEIVER received
		TO_PUBCOMP((byte) 2); // RECEIVER complete

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
			throw new IllegalArgumentException("invalid ReceiverTargetStatus: " + value);
		}
	}
}
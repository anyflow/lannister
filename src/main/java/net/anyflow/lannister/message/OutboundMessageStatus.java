package net.anyflow.lannister.message;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import net.anyflow.lannister.serialization.SerializableFactory;

public class OutboundMessageStatus extends MessageStatus {

	public static final int ID = 3;

	@JsonProperty
	private int inboundMessageId;
	@JsonProperty
	private Status targetStatus;

	public OutboundMessageStatus() { // just for Serialization
	}

	public OutboundMessageStatus(String clientId, int messageId, int inboundMessageId) {
		super(clientId, messageId);

		this.inboundMessageId = inboundMessageId;
		this.targetStatus = Status.TO_PUBLISH;
	}

	public int inboundMessageId() {
		return inboundMessageId;
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
		TO_PUBLISH((byte) 0),
		TO_PUBREL((byte) 1),
		TO_BE_REMOVED((byte) 2);

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
			throw new IllegalArgumentException("invalid SenderTargetStatus: " + value);
		}
	}
}
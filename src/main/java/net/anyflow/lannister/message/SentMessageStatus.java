package net.anyflow.lannister.message;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import net.anyflow.lannister.serialization.SerializableFactory;

public class SentMessageStatus extends MessageStatus {

	public static final int ID = 3;

	@JsonProperty
	private int originalMessageId;
	@JsonProperty
	private SenderTargetStatus targetStatus;

	public SentMessageStatus() { // just for Serialization
	}

	public SentMessageStatus(String clientId, int messageId, int originalMessageId) {
		super(clientId, messageId);

		this.originalMessageId = originalMessageId;
		this.targetStatus = SenderTargetStatus.TO_PUB;
	}

	public int originalMessageId() {
		return originalMessageId;
	}

	public SenderTargetStatus targetStatus() {
		return targetStatus;
	}

	public void targetStatus(SenderTargetStatus targetStatus) {
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

		targetStatus = SenderTargetStatus.valueOf(reader.readByte("targetStatus"));
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("clientId").addIntField("messageId")
				.addByteField("targetStatus").build();
	}

}
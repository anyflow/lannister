package net.anyflow.lannister.message;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import net.anyflow.lannister.SerializableFactory;

public class ReceivedMessageStatus extends MessageStatus {

	public static final int ID = 2;

	private ReceiverTargetStatus targetStatus;

	public ReceivedMessageStatus() { // just for Serialization
	}

	public ReceivedMessageStatus(String clientId, int messageId) {
		super(clientId, messageId);

		targetStatus = ReceiverTargetStatus.TO_ACK;
	}

	public ReceiverTargetStatus targetStatus() {
		return targetStatus;
	}

	public void targetStatus(ReceiverTargetStatus targetStatus) {
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

		targetStatus = ReceiverTargetStatus.valueOf(reader.readByte("targetStatus"));
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("clientId").addIntField("messageId")
				.addByteField("targetStatus").build();
	}
}
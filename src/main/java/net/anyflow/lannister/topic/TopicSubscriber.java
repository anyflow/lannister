package net.anyflow.lannister.topic;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import net.anyflow.lannister.Repository;
import net.anyflow.lannister.message.OutboundMessageStatus;
import net.anyflow.lannister.serialization.Jsonizable;
import net.anyflow.lannister.serialization.SerializableFactory;

public class TopicSubscriber extends Jsonizable implements com.hazelcast.nio.serialization.Portable {

	public final static int ID = 7;

	@JsonProperty
	private String clientId;
	@JsonProperty
	private String topicName;
	@JsonProperty
	private IMap<Integer, OutboundMessageStatus> outboundMessageStatuses; // KEY:messageId

	public TopicSubscriber() { // just for Serialization
	}

	protected TopicSubscriber(String clientId, String topicName) {
		this.clientId = clientId;
		this.topicName = topicName;
		this.outboundMessageStatuses = Repository.SELF.generator().getMap(messageStatusesName());
	}

	private String messageStatusesName() {
		return "TOPIC(" + topicName + ")_CLIENT(" + clientId + ")_outboundMessageStatuses";
	}

	public ImmutableMap<Integer, OutboundMessageStatus> sentOutboundMessageStatuses() {
		return ImmutableMap.copyOf(outboundMessageStatuses);
	}

	public void addOutboundMessageStatus(int messageId, int inboundMessageId,
			OutboundMessageStatus.Status targetStatus) {
		OutboundMessageStatus status = new OutboundMessageStatus(clientId, messageId, inboundMessageId);

		status.targetStatus(targetStatus);

		outboundMessageStatuses.put(status.messageId(), status);
	}

	public OutboundMessageStatus removeOutboundMessageStatus(int messageId) {
		return outboundMessageStatuses.remove(messageId);
	}

	public OutboundMessageStatus setOutboundMessageStatus(int messageId, OutboundMessageStatus.Status targetStatus) {
		OutboundMessageStatus status = outboundMessageStatuses.get(messageId);
		if (status == null) { return null; }

		status.targetStatus(targetStatus);

		return outboundMessageStatuses.put(status.messageId(), status);
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
		writer.writeUTF("topicName", topicName);
		writer.writeUTF("clientId", clientId);
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		topicName = reader.readUTF("topicName");
		clientId = reader.readUTF("clientId");

		outboundMessageStatuses = Repository.SELF.generator().getMap(messageStatusesName());
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("topicName").addUTFField("clientId")
				.build();
	}
}
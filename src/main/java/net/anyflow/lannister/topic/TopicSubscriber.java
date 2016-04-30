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
import net.anyflow.lannister.message.MessageStatus;
import net.anyflow.lannister.message.SenderTargetStatus;
import net.anyflow.lannister.message.SentMessageStatus;
import net.anyflow.lannister.serialization.Jsonizable;
import net.anyflow.lannister.serialization.SerializableFactory;

public class TopicSubscriber extends Jsonizable implements com.hazelcast.nio.serialization.Portable {

	public final static int ID = 7;

	@JsonProperty
	private String topicName;
	@JsonProperty
	private String clientId;
	@JsonProperty
	private transient IMap<Integer, SentMessageStatus> messageStatuses; // KEY:messageId

	public TopicSubscriber() { // just for Serialization
	}

	protected TopicSubscriber(String topicName, String clientId) {
		this.topicName = topicName;
		this.clientId = clientId;
		this.messageStatuses = Repository.SELF.generator().getMap(messageStatusesName());
	}

	private String messageStatusesName() {
		return "TOPIC(" + topicName + ")_CLIENT(" + clientId + ")_sentMessageStatuses";
	}

	public ImmutableMap<Integer, SentMessageStatus> sentMessageStatuses() {
		return ImmutableMap.copyOf(messageStatuses);
	}

	public void addSentMessageStatus(int messageId, int originalMessageId, SenderTargetStatus targetStatus) {
		SentMessageStatus status = new SentMessageStatus(clientId, messageId, originalMessageId);

		status.targetStatus(targetStatus);

		messageStatuses.put(status.messageId(), status);
	}

	public SentMessageStatus removeMessageStatus(int messageId) {
		return messageStatuses.remove(messageId);
	}

	public SentMessageStatus setSentMessageStatus(int messageId, SenderTargetStatus targetStatus) {
		SentMessageStatus status = messageStatuses.get(MessageStatus.key(clientId, messageId));
		if (status == null) { return null; }

		status.targetStatus(targetStatus);

		return messageStatuses.put(status.messageId(), status);
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

		messageStatuses = Repository.SELF.generator().getMap(messageStatusesName());
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("topicName").addUTFField("clientId")
				.build();
	}
}
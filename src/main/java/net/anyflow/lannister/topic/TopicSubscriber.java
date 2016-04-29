package net.anyflow.lannister.topic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.hazelcast.core.IMap;

import net.anyflow.lannister.Jsonizable;
import net.anyflow.lannister.Repository;
import net.anyflow.lannister.message.MessageStatus;
import net.anyflow.lannister.message.SenderTargetStatus;
import net.anyflow.lannister.message.SentMessageStatus;

public class TopicSubscriber extends Jsonizable implements java.io.Serializable {

	private static final long serialVersionUID = 5957543847054029282L;

	@JsonProperty
	private final String clientId;
	@JsonProperty
	private IMap<Integer, SentMessageStatus> messageStatuses; // KEY:messageId

	protected TopicSubscriber(String topicName, String clientId) {
		this.clientId = clientId;
		this.messageStatuses = Repository.SELF.generator()
				.getMap("TOPIC(" + topicName + ")_CLIENT(" + clientId + ")_sentMessageStatuses");
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
}
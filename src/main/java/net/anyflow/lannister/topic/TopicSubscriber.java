package net.anyflow.lannister.topic;

import com.fasterxml.jackson.annotation.JsonProperty;
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
	private IMap<Integer, SentMessageStatus> sentMessageStatuses; // KEY:messageId

	protected TopicSubscriber(String topicName, String clientId) {
		this.clientId = clientId;
		this.sentMessageStatuses = Repository.SELF.generator().getMap("TOPIC(" + topicName + ")_sentMessageStatuses");
	}

	protected IMap<Integer, SentMessageStatus> sentMessageStatuses() {
		return sentMessageStatuses;
	}

	public void addSentMessageStatus(int messageId, int originalMessageId, SenderTargetStatus targetStatus) {
		SentMessageStatus status = new SentMessageStatus(clientId, messageId, originalMessageId);

		status.targetStatus(targetStatus);

		sentMessageStatuses.put(status.messageId(), status);
	}

	public void setSentMessageStatus(int messageId, SenderTargetStatus targetStatus) {
		if (targetStatus == SenderTargetStatus.NOTHING) {
			sentMessageStatuses.remove(messageId);
			return;
		}

		SentMessageStatus status = sentMessageStatuses.get(MessageStatus.key(clientId, messageId));
		if (status == null) { return; }

		status.targetStatus(targetStatus);

		sentMessageStatuses.put(status.messageId(), status);
	}
}
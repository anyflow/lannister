package net.anyflow.lannister.topic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.hazelcast.core.IMap;

import net.anyflow.lannister.Jsonizable;
import net.anyflow.lannister.Repository;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.message.MessageStatus;
import net.anyflow.lannister.message.ReceivedMessageStatus;
import net.anyflow.lannister.message.ReceiverTargetStatus;
import net.anyflow.lannister.message.SentMessageStatus;
import net.anyflow.lannister.session.Session;

public class Topic extends Jsonizable implements java.io.Serializable {

	private static final long serialVersionUID = -3335949846595801533L;

	private static Topics TOPICS = new Topics();

	@JsonProperty
	private String name;
	@JsonProperty
	private Message retainedMessage;
	@JsonProperty
	private IMap<String, TopicSubscriber> subscribers; // clientIds
	@JsonProperty
	private IMap<String, Message> messages; // KEY:Message.key()
	@JsonProperty
	private IMap<String, ReceivedMessageStatus> receivedMessageStatuses; // KEY:clientId_messageId

	public Topic(String name) {
		this.name = name;
		this.retainedMessage = null;

		this.subscribers = Repository.SELF.generator().getMap("TOPIC(" + name + ")_subscribers");
		this.messages = Repository.SELF.generator().getMap("TOPIC(" + name + ")_messages");
		this.receivedMessageStatuses = Repository.SELF.generator()
				.getMap("TOPIC(" + name + ")_receivedMessageStatuses");
	}

	public String name() {
		return name;
	}

	public Message retainedMessage() {
		return retainedMessage;
	}

	public void setRetainedMessage(Message message) {
		this.retainedMessage = message;
		TOPICS.put(this);
	}

	public ImmutableMap<String, TopicSubscriber> subscribers() {
		return ImmutableMap.copyOf(subscribers);
	}

	public IMap<String, Message> messages() {
		return messages;
	}

	public void setReceivedMessageStatus(String clientId, int messageId, ReceiverTargetStatus targetStatus) {
		ReceivedMessageStatus status = receivedMessageStatuses.get(MessageStatus.key(clientId, messageId));
		if (status == null) {
			status = new ReceivedMessageStatus(clientId, messageId);
		}
		status.targetStatus(targetStatus);

		receivedMessageStatuses.put(status.key(), status);
	}

	public void addSubscriber(String clientId, boolean persist) {
		subscribers.put(clientId, new TopicSubscriber(clientId, name));

		if (persist) {
			TOPICS.put(this);
		}
	}

	public void removeSubscriber(String clientId, boolean persist) {
		subscribers.remove(clientId);

		// TODO should be this topic remained in spite of no subscriber?

		if (persist) {
			TOPICS.put(this);
		}
	}

	public void publish(Message message) {
		if (message.qos().value() > 0) {
			messages.put(message.key(), message);
		}

		subscribers.keySet().stream().parallel().forEach(item -> {
			Session session = Session.getByClientId(item, true);
			session.published(this, message);
		});
	}

	public static Topic get(String topicName) {
		return TOPICS.get(topicName);
	}

	public static Topic get(String clientId, int brokerMessageId) {
		return TOPICS.get(clientId, brokerMessageId);
	}

	public static boolean isValid(String topicName) {
		// TODO topic name validation
		return Strings.isNullOrEmpty(topicName) == false;
	}

	public static Topic put(Topic topic) {
		Session.topicAdded(topic);

		// TODO should be added in case of no subscriber & no retained Message?
		return TOPICS.put(topic);
	}

	public static void removeSubscribers(String topicFilter, String clientId, boolean persist) {
		TOPICS.removeSubscribers(topicFilter, clientId, persist);
	}

	public static SentMessageStatus messageAcked(String clientId, int messageId) {
		return TOPICS.messageAcked(clientId, messageId);
	}
}
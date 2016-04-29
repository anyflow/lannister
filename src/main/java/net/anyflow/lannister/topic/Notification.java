package net.anyflow.lannister.topic;

import net.anyflow.lannister.message.Message;

public class Notification implements java.io.Serializable {

	private static final long serialVersionUID = -5096104913288068061L;

	private String clientId;
	private Topic topic;
	private Message message;

	protected Notification(String clientId, Topic topic, Message message) {
		this.clientId = clientId;
		this.topic = topic;
		this.message = message;
	}

	public String clientId() {
		return clientId;
	}

	public Topic topic() {
		return topic;
	}

	public Message message() {
		return message;
	}
}
package net.anyflow.lannister.message;

import net.anyflow.lannister.Jsonizable;

public abstract class MessageStatus extends Jsonizable implements java.io.Serializable {
	private static final long serialVersionUID = 2335555073274982604L;

	private String clientId;
	private int messageId;

	protected MessageStatus(String clientId, int messageId) {
		this.clientId = clientId;
		this.messageId = messageId;
	}

	public String key() {
		return key(clientId, messageId);
	}

	protected String clientId() {
		return clientId;
	}

	public int messageId() {
		return messageId;
	}

	public static String key(String clientId, int messageId) {
		return clientId + "_" + Integer.toString(messageId);
	}
}
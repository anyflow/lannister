package net.anyflow.lannister.session;

import io.netty.util.internal.StringUtil;

public class MessageObject implements java.io.Serializable {

	private static final long serialVersionUID = -3661073065729414035L;

	private int id;
	private String topicName;
	private byte[] message;

	public MessageObject(int id, String topicName, byte[] message) {
		this.id = id;
		this.topicName = topicName;
		this.message = message;
	}

	public int id() {
		return id;
	}

	public String topicName() {
		return topicName;
	}

	public byte[] message() {
		return message;
	}

	@Override
	public String toString() {
		return new StringBuilder(StringUtil.simpleClassName(this)).append('[').append("id=").append(id)
				.append(", topeName=").append(topicName).append(", message=").append(new String(message)).append(']')
				.toString();
	}
}
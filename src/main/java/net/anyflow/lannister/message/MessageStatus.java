package net.anyflow.lannister.message;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import net.anyflow.lannister.Jsonizable;

public abstract class MessageStatus extends Jsonizable implements com.hazelcast.nio.serialization.Portable {

	@JsonProperty
	private String clientId;
	@JsonProperty
	private int messageId;

	public MessageStatus() { // just for Serialization
	}

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

	@Override
	public void writePortable(PortableWriter writer) throws IOException {
		writer.writeUTF("clientId", clientId);
		writer.writeInt("messageId", messageId);
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		clientId = reader.readUTF("clientId");
		messageId = reader.readInt("messageId");
	}
}
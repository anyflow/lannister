package net.anyflow.lannister.topic;

import java.io.IOException;

import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import net.anyflow.lannister.SerializableFactory;
import net.anyflow.lannister.message.Message;

public class Notification implements com.hazelcast.nio.serialization.Portable {
	public final static int ID = 5;

	private String clientId;
	private Topic topic;
	private Message message;

	public Notification() { // just for Serialization
	}

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

	@Override
	public int getFactoryId() {
		return SerializableFactory.ID;
	}

	@Override
	public int getClassId() {
		return ID;
	}

	@Override
	public void writePortable(PortableWriter writer) throws IOException {
		writer.writeUTF("clientId", clientId);

		if (topic != null) {
			writer.writePortable("topic", topic);
		}
		else {
			writer.writeNullPortable("topic", SerializableFactory.ID, Topic.ID);
		}

		if (message != null) {
			writer.writePortable("message", message);
		}
		else {
			writer.writeNullPortable("message", SerializableFactory.ID, Message.ID);
		}
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		clientId = reader.readUTF("clientId");
		topic = reader.readPortable("topic");
		message = reader.readPortable("message");
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("clientId")
				.addPortableField("topic", Topic.classDefinition())
				.addPortableField("message", Message.classDefinition()).build();
	}
}
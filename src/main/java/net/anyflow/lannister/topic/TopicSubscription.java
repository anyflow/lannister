package net.anyflow.lannister.topic;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.Jsonizable;
import net.anyflow.lannister.SerializableFactory;

public class TopicSubscription extends Jsonizable implements com.hazelcast.nio.serialization.Portable {

	public final static int ID = 8;

	@JsonProperty
	private String topicFilter;
	@JsonProperty
	private MqttQoS qos;

	public TopicSubscription() { // just for Serialization
	}

	public TopicSubscription(String topicFilter, MqttQoS qos) {
		this.topicFilter = topicFilter;
		this.qos = qos;
	}

	public String topicFilter() {
		return topicFilter;
	}

	public MqttQoS qos() {
		return qos;
	}

	public static boolean isValid(String topicFilter) {
		// TODO topic filter validation
		return Strings.isNullOrEmpty(topicFilter) == false;
	}

	public boolean isMatch(String topicName) {
		return isMatch(topicFilter, topicName);
	}

	public static boolean isMatch(String topicFilter, String topicName) {
		if (Strings.isNullOrEmpty(topicFilter) || Strings.isNullOrEmpty(topicName)) { return false; }

		// TODO topic wildcard filtering

		return topicFilter.equals(topicName);
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
		writer.writeUTF("topicFilter", topicFilter);
		writer.writeInt("qos", qos.value());
	}

	@Override
	public void readPortable(PortableReader reader) throws IOException {
		topicFilter = reader.readUTF("topicFilter");
		qos = MqttQoS.valueOf(reader.readInt("qos"));
	}

	public static ClassDefinition classDefinition() {
		return new ClassDefinitionBuilder(SerializableFactory.ID, ID).addUTFField("topicFilter").addIntField("qos")
				.build();
	}
}
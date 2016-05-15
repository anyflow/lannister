/*
 * Copyright 2016 The Lannister Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.anyflow.lannister.topic;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.plugin.ITopicSubscription;
import net.anyflow.lannister.serialization.SerializableFactory;

public class TopicSubscription implements com.hazelcast.nio.serialization.Portable, ITopicSubscription {

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

	/* (non-Javadoc)
	 * @see net.anyflow.lannister.topic.ITopicSubscription#topicFilter()
	 */
	@Override
	public String topicFilter() {
		return topicFilter;
	}

	/* (non-Javadoc)
	 * @see net.anyflow.lannister.topic.ITopicSubscription#qos()
	 */
	@Override
	public MqttQoS qos() {
		return qos;
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
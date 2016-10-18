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
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import net.anyflow.lannister.serialization.SerializableFactory;

public class TopicSubscriber implements com.hazelcast.nio.serialization.IdentifiedDataSerializable {
	public static final TopicSubscribers NEXUS = new TopicSubscribers();
	public static final int ID = 7;
	@JsonProperty
	private String clientId;
	@JsonProperty
	private String topicName;

	public TopicSubscriber() { // just for Serialization
	}

	public TopicSubscriber(String clientId, String topicName) {
		this.clientId = clientId;
		this.topicName = topicName;
	}

	public String key() {
		return TopicSubscribers.key(topicName, clientId);
	}

	public String clientId() {
		return clientId;
	}

	public String topicName() {
		return topicName;
	}

	@JsonIgnore
	@Override
	public int getFactoryId() {
		return SerializableFactory.ID;
	}

	@JsonIgnore
	@Override
	public int getId() {
		return ID;
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(topicName);
		out.writeUTF(clientId);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		topicName = in.readUTF();
		clientId = in.readUTF();
	}

	@Override
	public String toString() {
		return new StringBuilder().append("clientId=").append(clientId).append(", topicName=").append(topicName)
				.toString();
	}
}
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

package net.anyflow.lannister.message;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import net.anyflow.lannister.serialization.Jsonizable;

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
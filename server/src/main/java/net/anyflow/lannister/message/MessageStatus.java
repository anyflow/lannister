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
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import net.anyflow.lannister.Literals;

public abstract class MessageStatus implements com.hazelcast.nio.serialization.IdentifiedDataSerializable {

	@JsonProperty
	private String messageKey;
	@JsonProperty
	private String clientId;
	@JsonProperty
	private int messageId;
	@JsonProperty
	private String topicName;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	private Date createTime;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Literals.DATE_DEFAULT_FORMAT, timezone = Literals.DATE_DEFAULT_TIMEZONE)
	@JsonProperty
	protected Date updateTime;

	public MessageStatus() { // just for Serialization
	}

	protected MessageStatus(String messageKey, String clientId, int messageId, String topicName) {
		this.messageKey = messageKey;
		this.clientId = clientId;
		this.messageId = messageId;
		this.topicName = topicName;
		this.createTime = new Date();
		this.updateTime = createTime;
	}

	public abstract String key();

	public String messageKey() {
		return messageKey;
	}

	public String clientId() {
		return clientId;
	}

	public String topicName() {
		return topicName;
	}

	public int messageId() {
		return messageId;
	}

	public Date createTime() {
		return createTime;
	}

	public Date updateTime() {
		return updateTime;
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(messageKey);
		out.writeUTF(clientId);
		out.writeInt(messageId);
		out.writeUTF(topicName);
		out.writeLong(createTime != null ? createTime.getTime() : Long.MIN_VALUE);
		out.writeLong(updateTime != null ? updateTime.getTime() : Long.MIN_VALUE);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		messageKey = in.readUTF();
		clientId = in.readUTF();
		messageId = in.readInt();
		topicName = in.readUTF();

		long rawLong = in.readLong();
		createTime = rawLong != Long.MIN_VALUE ? new Date(rawLong) : null;

		rawLong = in.readLong();
		updateTime = rawLong != Long.MIN_VALUE ? new Date(rawLong) : null;
	}
}
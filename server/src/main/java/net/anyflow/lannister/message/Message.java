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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import net.anyflow.lannister.NettyUtil;
import net.anyflow.lannister.plugin.IMessage;
import net.anyflow.lannister.serialization.SerializableFactory;

public class Message implements com.hazelcast.nio.serialization.IdentifiedDataSerializable, IMessage, Cloneable {
	public final static Messages NEXUS = new Messages();
	public final static int ID = 1;

	public static final int MAX_MESSAGE_ID_NUM = 0xffff;
	public static final int MIN_MESSAGE_ID_NUM = 1;

	@JsonProperty
	private int id;
	@JsonProperty
	private String topicName;
	@JsonProperty
	private String publisherId;
	@JsonProperty
	private byte[] message;
	@JsonProperty
	private MqttQoS qos;
	@JsonProperty
	private boolean isRetain;

	public Message() {
	}

	public Message(ObjectDataInput in) throws IOException {
		readData(in);
	}

	public Message(int id, String topicName, String publisherId, byte[] message, MqttQoS qos, boolean isRetain) {
		this.id = id;
		this.topicName = topicName;
		this.publisherId = publisherId;
		this.message = message != null ? message : new byte[] {};
		this.qos = qos;
		this.isRetain = isRetain;
	}

	public String key() {
		return Messages.key(publisherId, id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.lannister.message.IMessage#id()
	 */
	@Override
	public int id() {
		return id;
	}

	public void id(int id) {
		this.id = id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.lannister.message.IMessage#topicName()
	 */
	@Override
	public String topicName() {
		return topicName;
	}

	@Override
	public String publisherId() {
		return publisherId;
	}

	public void publisherId(String publisherId) {
		this.publisherId = publisherId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.lannister.message.IMessage#message()
	 */
	@Override
	public byte[] message() {
		return message;
	}

	public void setMessage(byte[] message) {
		this.message = message != null ? message : new byte[] {};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.lannister.message.IMessage#qos()
	 */
	@Override
	public MqttQoS qos() {
		return qos;
	}

	public void qos(MqttQoS qos) {
		this.qos = qos;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.anyflow.lannister.message.IMessage#isRetain()
	 */
	@Override
	public boolean isRetain() {
		return isRetain;
	}

	public void setRetain(boolean isRetain) {
		this.isRetain = isRetain;
	}

	@Override
	public String toString() {
		return new StringBuilder(StringUtil.simpleClassName(this)).append('[').append("id=").append(id)
				.append(", topeName=").append(topicName).append(", message=")
				.append(new String(message, CharsetUtil.UTF_8)).append(", QoS=").append(qos).append(", retain=")
				.append(isRetain).append(']').toString();
	}

	@Override
	public Message clone() {
		return new Message(id, topicName, publisherId, message, qos, isRetain);
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
		out.writeInt(id);
		out.writeUTF(topicName);
		out.writeUTF(publisherId);
		out.writeByteArray(message);
		out.writeInt(qos != null ? qos.value() : Byte.MIN_VALUE);
		out.writeBoolean(isRetain);
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		id = in.readInt();
		topicName = in.readUTF();
		publisherId = in.readUTF();
		message = in.readByteArray();

		int rawInt = in.readInt();
		qos = rawInt != Byte.MIN_VALUE ? MqttQoS.valueOf(rawInt) : null;

		isRetain = in.readBoolean();
	}

	public static Message newMessage(String clientId, MqttPublishMessage published) {
		return new Message(published.variableHeader().messageId(), published.variableHeader().topicName(), clientId,
				NettyUtil.copy(published.payload()), published.fixedHeader().qosLevel(),
				published.fixedHeader().isRetain());
	}
}
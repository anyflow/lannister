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

package net.anyflow.lannister.plugin;

import java.util.Arrays;

import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * Represent MQTT message.
 */
public interface IMessage {
	/**
	 * @return message identifier(packet identifier in MQTT 3.1.1)
	 */
	int id();

	/**
	 * @return topic's name the message belongs to
	 */
	String topicName();

	/**
	 * @return client identifier whose client published the message
	 */
	String publisherId();

	/**
	 * @return message itself
	 */
	byte[] message();

	/**
	 * @return message QoS. The value can be used different in client publishing
	 *         time and broker publishing time
	 */
	MqttQoS qos();

	/**
	 * @return whether the message is retained or not
	 */
	boolean isRetain();

	default String log() {
		return (new StringBuilder()).append("messageId=").append(id()).append(", topicName=").append(topicName())
				.append(", publisherId=").append(publisherId()).append(", message=").append(Arrays.toString(message()))
				.append(", qos=").append(qos()).append(", isRetain=").append(isRetain()).toString();
	}
}
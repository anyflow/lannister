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

import io.netty.handler.codec.mqtt.MqttQoS;

public interface IMessage {

	int id();

	void setId(int id);

	String topicName();

	String publisherId();

	void publisherId(String publisherId);

	byte[] message();

	void setMessage(byte[] message);

	MqttQoS qos();

	void setQos(MqttQoS qos);

	boolean isRetain();

	void setRetain(boolean isRetain);
}
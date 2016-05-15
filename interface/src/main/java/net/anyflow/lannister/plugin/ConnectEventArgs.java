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

import net.anyflow.lannister.entity.MqttConnectReturnCode;
import net.anyflow.lannister.message.IMessage;

public interface ConnectEventArgs {
	String clientId();

	IMessage will();

	Boolean cleanSession();

	MqttConnectReturnCode returnCode();

	default public String log() {
		return (new StringBuilder()).append("clientId=").append(clientId()).append(", will=").append(will())
				.append(", cleanSession=").append(cleanSession()).append(", returnCode=").append(returnCode())
				.toString();
	}
}
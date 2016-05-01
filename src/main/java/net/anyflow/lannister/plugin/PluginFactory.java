package net.anyflow.lannister.plugin;

import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;

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

public class PluginFactory {

	public Plugin create(Class<? extends Plugin> pluginType) {
		// TODO Retrieve client object's plugin
		// TODO Getting default value from application.properties

		if (EventListener.class.getTypeName().equals(pluginType.getTypeName())) {
			return new EventListener() {
				@Override
				public void connectMessageReceived(MqttConnectMessage msg) {
					return;
				}

				@Override
				public void connAckMessageSent(MqttConnAckMessage msg) {
					return;
				}
			};
		}
		else if (Authorization.class.getTypeName().equals(pluginType.getTypeName())) {
			return new Authorization() {
				@Override
				public boolean isValid(String clientId) {
					return true;
				}

				@Override
				public boolean isValid(boolean hasUserName, boolean hasPassword, String userName, String password) {
					return true;
				}

				@Override
				public boolean isAuthorized(boolean hasUserName, String username) {
					return true;
				}
			};
		}
		else if (ServiceStatus.class.getTypeName().equals(pluginType.getTypeName())) {
			return new ServiceStatus() {
				@Override
				public boolean isServiceAvailable() {
					return true;
				}
			};
		}
		else {
			return null;
		}
	}
}
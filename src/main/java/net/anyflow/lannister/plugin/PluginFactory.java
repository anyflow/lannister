package net.anyflow.lannister.plugin;

import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;

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
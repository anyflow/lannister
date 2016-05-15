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
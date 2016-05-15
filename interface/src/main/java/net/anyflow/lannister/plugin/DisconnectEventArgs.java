package net.anyflow.lannister.plugin;

public interface DisconnectEventArgs {
	String clientId();

	Boolean cleanSession();

	Boolean byDisconnectMessage();

	default public String log() {
		return (new StringBuilder()).append("clientId=").append(clientId()).append(", cleanSession=")
				.append(cleanSession()).append(", byDisconnectMessage=").append(byDisconnectMessage()).toString();
	}
}
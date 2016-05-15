package net.anyflow.lannister.entity;

import net.anyflow.lannister.message.IMessage;

public class ConnectEventArgs {
	private String clientId;
	private IMessage will;
	private boolean cleanSession;
	private MqttConnectReturnCode returnCode;

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public IMessage getWill() {
		return will;
	}

	public void setWill(IMessage will) {
		this.will = will;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public MqttConnectReturnCode returnCode() {
		return returnCode;
	}
}
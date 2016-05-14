package net.anyflow.lannister.message;

import io.netty.handler.codec.mqtt.MqttVersion;

public class ConnectOptions {
	private MqttVersion version;
	private String clientId;
	private String userName;
	private String password;
	private boolean cleanSession;
	private IMessage will;
	private int keepAliveTimeSeconds;

	public ConnectOptions() {
		this.version = MqttVersion.MQTT_3_1_1;
		this.clientId = null;
		this.cleanSession = true;
		this.will = null;
		this.userName = null;
		this.password = null;
		this.keepAliveTimeSeconds = 120;
	}

	public ConnectOptions(MqttVersion version, String clientId, boolean cleanSession, IMessage will, String userName,
			String password, int keepAliveTimeSeconds) {
		this.version = version;
		this.clientId = clientId;
		this.cleanSession = cleanSession;
		this.will = will;
		this.userName = userName;
		this.password = password;
		this.keepAliveTimeSeconds = keepAliveTimeSeconds;
	}

	public MqttVersion version() {
		return version;
	}

	public void version(MqttVersion version) {
		this.version = version;
	}

	public String clientId() {
		return clientId;
	}

	public void clientId(String clientId) {
		this.clientId = clientId;
	}

	public String userName() {
		return userName;
	}

	public void userName(String userName) {
		this.userName = userName;
	}

	public String password() {
		return password;
	}

	public void password(String password) {
		this.password = password;
	}

	public boolean cleanSession() {
		return cleanSession;
	}

	public void cleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public IMessage will() {
		return will;
	}

	public void will(IMessage will) {
		this.will = will;
	}

	public int keepAliveTimeSeconds() {
		return keepAliveTimeSeconds;
	}

	public void keepAliveTimeSeconds(int keepAliveTimeSeconds) {
		this.keepAliveTimeSeconds = keepAliveTimeSeconds;
	}
}
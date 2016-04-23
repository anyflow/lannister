package net.anyflow.lannister.session;

import io.netty.handler.codec.mqtt.MqttQoS;

public class SessionTopic implements java.io.Serializable {

	private static final long serialVersionUID = -3335949846595801533L;

	private MqttQoS qos;
	private String registrationId;
	private Message retainedMessage;

	public SessionTopic(String registrationId, MqttQoS qos) {
		this.registrationId = registrationId;
		this.qos = qos;
	}

	public MqttQoS qos() {
		return qos;
	}

	public String registrationId() {
		return registrationId;
	}

	public Message retainedMessage() {
		return retainedMessage;
	}

	public void setRetainedMesage(Message retainedMessage) {
		this.retainedMessage = retainedMessage;
	}
}
package net.anyflow.lannister.session;

import io.netty.handler.codec.mqtt.MqttQoS;

public class TopicRegister {
	private MqttQoS qos;
	private String registrationId;

	public TopicRegister(String registrationId, MqttQoS qos) {
		this.registrationId = registrationId;
		this.qos = qos;
	}

	public MqttQoS qos() {
		return qos;
	}

	public String registrationId() {
		return registrationId;
	}
}
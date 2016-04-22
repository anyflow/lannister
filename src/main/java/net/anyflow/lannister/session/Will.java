package net.anyflow.lannister.session;

import io.netty.handler.codec.mqtt.MqttQoS;

public class Will implements java.io.Serializable {
	private static final long serialVersionUID = 5516350955535420440L;

	private String topic;
	private String message;
	private MqttQoS qos;
	private boolean retained;

	public Will(String topic, String message, MqttQoS qos, boolean retained) {
		this.topic = topic;
		this.message = message;
	}

	public String topic() {
		return topic;
	}

	public String message() {
		return message;
	}

	public MqttQoS qos() {
		return qos;
	}

	public boolean retained() {
		return retained;
	}
}
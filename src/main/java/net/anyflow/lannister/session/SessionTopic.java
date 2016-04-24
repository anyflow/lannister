package net.anyflow.lannister.session;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.Jsonizable;

public class SessionTopic extends Jsonizable implements java.io.Serializable {

	private static final long serialVersionUID = -3335949846595801533L;

	@JsonProperty
	private String registrationId;
	@JsonProperty
	private String name;
	@JsonProperty
	private MqttQoS qos;
	@JsonProperty
	private Message retainedMessage;

	public SessionTopic(String registrationId, String name, MqttQoS qos) {
		this.registrationId = registrationId;
		this.name = name;
		this.qos = qos;
	}

	public String registrationId() {
		return registrationId;
	}

	public String name() {
		return name;
	}

	public MqttQoS qos() {
		return qos;
	}

	public Message retainedMessage() {
		return retainedMessage;
	}

	public void setRetainedMesage(Message retainedMessage) {
		this.retainedMessage = retainedMessage;
	}
}
package net.anyflow.lannister.session;

import java.util.Map;

import com.google.common.collect.Maps;

import io.netty.handler.codec.mqtt.MqttQoS;

public class SessionTopic implements java.io.Serializable {

	private static final long serialVersionUID = -3335949846595801533L;

	private MqttQoS qos;
	private String registrationId;
	private Message retainedMessage;

	private Map<Integer, Message> messages;

	public SessionTopic(String registrationId, MqttQoS qos) {
		this.registrationId = registrationId;
		this.qos = qos;
		this.messages = Maps.newConcurrentMap();
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

	public Map<Integer, Message> messages() {
		return messages;
	}
}
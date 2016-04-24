package net.anyflow.lannister.session;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.internal.StringUtil;
import net.anyflow.lannister.Jsonizable;

public class Message extends Jsonizable implements java.io.Serializable {

	private static final long serialVersionUID = -3661073065729414035L;

	@JsonProperty
	private Integer id;
	@JsonProperty
	private String topicName;
	@JsonProperty
	private byte[] message;
	@JsonProperty
	private MqttQoS qos;
	@JsonProperty
	private boolean isRetain;
	@JsonProperty
	private boolean sent;

	public Message(Integer id, String topicName, byte[] message, MqttQoS qos, boolean isRetain) {
		this.id = id;
		this.topicName = topicName;
		this.message = message;
		this.qos = qos;
		this.isRetain = isRetain;
		this.sent = false;
	}

	public int id() {
		return id;
	}

	public String topicName() {
		return topicName;
	}

	public byte[] message() {
		return message;
	}

	public void setMessage(byte[] message) {
		this.message = message;
	}

	public MqttQoS qos() {
		return qos;
	}

	public void setQos(MqttQoS qos) {
		this.qos = qos;
	}

	public boolean isRetain() {
		return isRetain;
	}

	public boolean isSent() {
		return sent;
	}

	public void setSent(boolean sent) {
		this.sent = sent;
	}

	@Override
	public String toString() {
		return new StringBuilder(StringUtil.simpleClassName(this)).append('[').append("id=").append(id)
				.append(", topeName=").append(topicName).append(", message=").append(new String(message))
				.append(", QoS=").append(qos).append(", retain=").append(isRetain).append(']').toString();
	}
}
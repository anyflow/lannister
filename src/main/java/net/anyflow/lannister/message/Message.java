package net.anyflow.lannister.message;

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
	private String publisherId;
	@JsonProperty
	private byte[] message;
	@JsonProperty
	private MqttQoS qos;
	@JsonProperty
	private boolean isRetain;

	public Message(Integer id, String topicName, String publisherId, byte[] message, MqttQoS qos, boolean isRetain) {
		this.id = id;
		this.topicName = topicName;
		this.publisherId = publisherId;
		this.message = message != null ? message : new byte[] {};
		this.qos = qos;
		this.isRetain = isRetain;
	}

	public int id() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String topicName() {
		return topicName;
	}

	public String publisherId() {
		return publisherId;
	}

	public byte[] message() {
		return message;
	}

	public void setMessage(byte[] message) {
		this.message = message != null ? message : new byte[] {};
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

	public void setRetain(boolean isRetain) {
		this.isRetain = isRetain;
	}

	@Override
	public String toString() {
		return new StringBuilder(StringUtil.simpleClassName(this)).append('[').append("id=").append(id)
				.append(", topeName=").append(topicName).append(", message=").append(new String(message))
				.append(", QoS=").append(qos).append(", retain=").append(isRetain).append(']').toString();
	}

	public String key() {
		return publisherId + "_" + Integer.toString(id);
	}
}
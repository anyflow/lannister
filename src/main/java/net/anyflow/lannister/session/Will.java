package net.anyflow.lannister.session;

import io.netty.handler.codec.mqtt.MqttQoS;

public class Will implements java.io.Serializable {

	// Situations in which the Will Message is published include, but are not
	// limited to:
	// An I/O error or network failure detected by the Server.
	// The Client fails to communicate within the Keep Alive time.
	// The Client closes the Network Connection without first sending a
	// DISCONNECT Packet.
	// The Server closes the Network Connection because of a protocol error.
	//
	// If the Will Flag is set to 1, the Will QoS and Will Retain fields in the
	// Connect Flags will be used by the Server, and the Will Topic and Will
	// Message fields MUST be present in the payload [MQTT-3.1.2-9].
	// The Will Message MUST be removed from the stored Session state in the
	// Server once it has been published or the Server has received a DISCONNECT
	// packet from the Client [MQTT-3.1.2-10].
	// If the Will Flag is set to 0 the Will QoS and Will Retain fields in the
	// Connect Flags MUST be set to zero and the Will Topic and Will Message
	// fields MUST NOT be present in the payload [MQTT-3.1.2-11].
	// If the Will Flag is set to 0, a Will Message MUST NOT be published when
	// this Network Connection ends [MQTT-3.1.2-12].

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
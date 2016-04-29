package net.anyflow.lannister.topic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.Jsonizable;

public class TopicSubscription extends Jsonizable implements java.io.Serializable {

	private static final long serialVersionUID = -3335949846595801533L;

	@JsonProperty
	private String topicFilter;
	@JsonProperty
	private MqttQoS qos;

	public TopicSubscription(String topicFilter, MqttQoS qos) {
		this.topicFilter = topicFilter;
		this.qos = qos;
	}

	public String topicFilter() {
		return topicFilter;
	}

	public MqttQoS qos() {
		return qos;
	}

	public static boolean isValid(String topicFilter) {
		// TODO topic filter validation
		return Strings.isNullOrEmpty(topicFilter) == false;
	}

	public boolean isMatch(String topicName) {
		return isMatch(topicFilter, topicName);
	}

	public static boolean isMatch(String topicFilter, String topicName) {
		if (Strings.isNullOrEmpty(topicFilter) || Strings.isNullOrEmpty(topicName)) { return false; }

		// TODO topic wildcard filtering

		return topicFilter.equals(topicName);
	}
}
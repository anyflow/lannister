package net.anyflow.lannister.plugin;

import io.netty.handler.codec.mqtt.MqttQoS;

public interface ITopicSubscription {

	String topicFilter();

	MqttQoS qos();

	default String log() {
		return (new StringBuilder()).append("topicFilter=").append(topicFilter()).append(", qos=").append(qos())
				.toString();
	}
}
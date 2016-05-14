package net.anyflow.lannister.client;

import io.netty.handler.codec.mqtt.MqttMessage;

public class SharedObject {
	private MqttMessage receivedMessage;
	private Object locker;

	public SharedObject() {
		receivedMessage = null;
		locker = new Object();
	}

	public Object locker() {
		return locker;
	}

	public MqttMessage receivedMessage() {
		return receivedMessage;
	}

	public void receivedMessage(MqttMessage receivedMessage) {
		this.receivedMessage = receivedMessage;
	}
}
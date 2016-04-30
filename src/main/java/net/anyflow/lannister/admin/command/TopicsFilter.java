package net.anyflow.lannister.admin.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.topic.Topic;

public class TopicsFilter implements MessageFilter {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TopicsFilter.class);

	private byte[] all() {
		try {
			return (new ObjectMapper()).writeValueAsBytes(Topic.NEXUS.map());
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public void execute(Message message) {
		if (message == null || message.topicName().startsWith("$COMMAND/GET/topics") == false) { return; }

		message.setQos(MqttQoS.AT_MOST_ONCE);

		if (message.topicName().equals("$COMMAND/GET/topics")) {
			message.setMessage(all());
		}
	}
}

package net.anyflow.lannister.admin.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.menton.http.HttpRequestHandler;

@HttpRequestHandler.Handles(paths = { "topics" }, httpMethods = { "GET" })
public class TopicsFilter extends HttpRequestHandler implements MessageFilter {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TopicsFilter.class);

	private byte[] allBinary() {
		try {
			return (new ObjectMapper()).writeValueAsBytes(Topic.NEXUS.map());
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private String allString() {
		try {
			return (new ObjectMapper()).writeValueAsString(Topic.NEXUS.map());
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
			message.setMessage(allBinary());
		}
	}

	@Override
	public String service() {
		String filter = Strings.nullToEmpty(httpRequest().parameter("filter"));

		switch (filter) {
		case "":
		case "all":
			return allString();

		default:
			return null;
		}
	}
}

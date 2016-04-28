package net.anyflow.lannister.admin.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.session.Session;

public class SessionsFilter implements MessageFilter {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SessionsFilter.class);

	private byte[] live() {
		try {
			return (new ObjectMapper()).writeValueAsBytes(net.anyflow.lannister.session.Session.clientIdMap(false));
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private byte[] persisted() {
		try {
			return (new ObjectMapper()).writeValueAsBytes(Session.persistedClientIdMap());
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	@Override
	public void execute(Message message) {
		if (message == null || message.topicName().startsWith("$COMMAND/GET/sessions") == false) { return; }

		message.setQos(MqttQoS.AT_MOST_ONCE);

		if (message.topicName().equals("$COMMAND/GET/sessions")) {
			message.setMessage(live());
		}
		else if (message.topicName().equals("$COMMAND/GET/sessions?filter=live")) {
			message.setMessage(live());
		}
		else if (message.topicName().equals("$COMMAND/GET/sessions?filter=persisted")) {
			message.setMessage(persisted());
		}
	}
}
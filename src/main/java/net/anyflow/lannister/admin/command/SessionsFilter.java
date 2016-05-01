package net.anyflow.lannister.admin.command;

import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import io.netty.handler.codec.mqtt.MqttQoS;
import net.anyflow.lannister.message.Message;
import net.anyflow.lannister.session.Session;
import net.anyflow.menton.http.HttpRequestHandler;

@HttpRequestHandler.Handles(paths = { "sessions" }, httpMethods = { "GET" })
public class SessionsFilter extends HttpRequestHandler implements MessageFilter {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SessionsFilter.class);

	private byte[] liveBinary() {
		try {
			return (new ObjectMapper()).writeValueAsBytes(Session.NEXUS.map().values().stream()
					.filter(s -> s.isConnected()).collect(Collectors.toMap(Session::clientId, Function.identity())));
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private byte[] allBinary() {
		try {
			return (new ObjectMapper()).writeValueAsBytes(Session.NEXUS.map());
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private String liveString() {
		try {
			return (new ObjectMapper()).writeValueAsString(Session.NEXUS.map().values().stream()
					.filter(s -> s.isConnected()).collect(Collectors.toMap(Session::clientId, Function.identity())));
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private String allString() {
		try {
			return (new ObjectMapper()).writeValueAsString(Session.NEXUS.map());
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
			message.setMessage(allBinary());
		}
		else if (message.topicName().equals("$COMMAND/GET/sessions?filter=live")) {
			message.setMessage(liveBinary());
		}
		else if (message.topicName().equals("$COMMAND/GET/sessions?filter=all")) {
			message.setMessage(allBinary());
		}
	}

	@Override
	public String service() {
		String filter = Strings.nullToEmpty(httpRequest().parameter("filter"));

		switch (filter) {
		case "":
		case "live":
			return liveString();

		case "all":
			return allString();

		default:
			return null;
		}
	}
}
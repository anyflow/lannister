package net.anyflow.lannister.admin.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.anyflow.lannister.plugin.Plugin;
import net.anyflow.lannister.session.LiveSessions;
import net.anyflow.lannister.session.Repository;

public class Sessions implements Plugin {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Sessions.class);

	public byte[] live() {
		try {
			return (new ObjectMapper()).writeValueAsBytes(LiveSessions.SELF.clientIdMap());
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	public byte[] persisted() {
		try {
			return (new ObjectMapper()).writeValueAsBytes(Repository.SELF.clientIdSessionMap());
		}
		catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}
}
package net.anyflow.lannister;

import java.io.IOException;

import org.apache.log4j.lf5.LogLevel;

public class Settings extends java.util.Properties {

	private static final long serialVersionUID = -3232325711649130464L;

	protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Settings.class);

	public final static Settings SELF;

	static {
		SELF = new Settings();
	}

	private Settings() {
		try {
			load(Application.class.getClassLoader().getResourceAsStream("META-INF/application.properties"));
		}
		catch (IOException e) {
			logger.error("Settings instantiation failed.", e);
		}
	}

	public int getInt(String key, int defaultValue) {
		String valueString = this.getProperty(key.trim());

		if (valueString == null) { return defaultValue; }

		try {
			return Integer.parseInt(valueString.trim());
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		String valueString = this.getProperty(key.trim());

		if (valueString == null) { return defaultValue; }

		return "true".equalsIgnoreCase(valueString.trim());
	}

	public LogLevel logLevel() {
		if (logger.isDebugEnabled()) {
			return LogLevel.DEBUG;
		}
		else if (logger.isInfoEnabled()) {
			return LogLevel.INFO;
		}
		else if (logger.isWarnEnabled()) {
			return LogLevel.WARN;
		}
		else {
			return LogLevel.DEBUG;
		}
	}
}
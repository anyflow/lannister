package net.anyflow.lannister;

public class Application {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {

		logger.debug("application started.", Settings.SELF.getProperty("", ""));
	}
}
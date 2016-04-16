package net.anyflow.lannister;

public class Application {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Application.class);

	private static Server server;

	public static void main(String[] args) {

		org.apache.log4j.xml.DOMConfigurator
				.configure(Application.class.getClassLoader().getResource("META-INF/log4j.xml"));

		server = new Server();
		server.Start();

		logger.debug("Lannister bootstrapping finished successfully.");
	}
}
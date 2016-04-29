package net.anyflow.lannister;

public class Application {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Application.class);

	private static Server server;

	public static void main(String[] args) {

		org.apache.log4j.xml.DOMConfigurator
				.configure(Application.class.getClassLoader().getResource("META-INF/log4j.xml"));

		logger.info("Lannister bootstrapping started.");

		server = new Server();
		server.Start();

		// TODO dispose session
		// TODO Dispose topic
		// TODO dispose message

		// TODO stream parellel
		// TODO TEST revive persisted session
		// TODO exception handling thrown by codec
		// TODO Establish removing polishes of unused old persistent sessions,
		// topics.
		// TODO TEST will sending

		// TODO $SYS
		// TODO wildcard support
		// TODO QoS2 support
		// TODO WebSocket
		// TODO SSL
		// TODO importing menton => netty ver 4.1 upgrade
		// TODO JMX?
	}
}
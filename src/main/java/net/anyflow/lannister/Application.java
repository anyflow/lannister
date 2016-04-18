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

		// TODO $SYS
		// TODO importing menton => netty ver 4.1 upgrade
		// TODO JMX?
		// TODO wildcard support
		// TODO QoS managing
		// TODO QoS2 support
		// TODO Version 3.1.1 support checking
		// TODO WebSocket
		// TODO SSL

		// TODO session state
		// · The existence of a Session, even if the rest of the Session state
		// is empty.
		// · The Client’s subscriptions.
		// · QoS 1 and QoS 2 messages which have been sent to the Client, but
		// have not been completely acknowledged.
		// · QoS 1 and QoS 2 messages pending transmission to the Client.
		// · QoS 2 messages which have been received from the Client, but have
		// not been completely acknowledged.
		// · Optionally, QoS 0 messages pending transmission to the Client.
	}
}
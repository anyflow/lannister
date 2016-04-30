package net.anyflow.lannister;

import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.session.Sessions;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.Topics;

public class Application {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Application.class);

	private static Server server;

	public static void main(String[] args) {

		org.apache.log4j.xml.DOMConfigurator
				.configure(Application.class.getClassLoader().getResource("META-INF/log4j.xml"));

		logger.info("Lannister bootstrapping started.");

		Session.NEXUS = new Sessions();
		Topic.NEXUS = new Topics(Session.NEXUS);

		server = new Server();
		server.Start();

		// TODO dispose session
		// TODO Dispose topic
		// TODO dispose message

		// TODO stream parallel
		// TODO exception handling thrown by codec
		// TODO Removing polishes of unused old persistent sessions/topics.
		// TODO TEST will sending

		// TODO specification TEST
		// TODO $SYS
		// TODO wildcard support
		// TODO WebSocket
		// TODO SSL
		// TODO importing menton => netty ver 4.1 upgrade
		// TODO JMX?
		// TODO discard QoS0 retained message (server decision [MQTT-3.3.1-7])
	}
}
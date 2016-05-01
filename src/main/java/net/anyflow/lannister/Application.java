package net.anyflow.lannister;

import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.session.Sessions;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.Topics;
import net.anyflow.menton.http.WebServer;

public class Application {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Application.class);

	private static MqttServer mqttServer;
	private static WebServer webServer;

	public static void main(String[] args) {

		org.apache.log4j.xml.DOMConfigurator
				.configure(Application.class.getClassLoader().getResource("META-INF/log4j.xml"));

		logger.info("Lannister bootstrapping started.");

		Session.NEXUS = new Sessions();
		Topic.NEXUS = new Topics(Session.NEXUS);

		mqttServer = new MqttServer();
		mqttServer.Start();

		webServer = new WebServer();
		webServer.start("net.anyflow");
	}
}
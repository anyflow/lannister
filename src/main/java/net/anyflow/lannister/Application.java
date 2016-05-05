/*
 * Copyright 2016 The Lannister Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

	public static void configureLog4j() {
		org.apache.log4j.xml.DOMConfigurator
				.configure(Application.class.getClassLoader().getResource("META-INF/log4j.xml"));
	}
}
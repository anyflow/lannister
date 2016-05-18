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

import net.anyflow.lannister.server.MqttServer;
import net.anyflow.lannister.session.Session;
import net.anyflow.lannister.session.Sessions;
import net.anyflow.lannister.topic.Topic;
import net.anyflow.lannister.topic.Topics;
import net.anyflow.menton.http.WebServer;

public class Application {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Application.class);

	private static Application INSTANCE;

	private MqttServer mqttServer;
	private WebServer webServer;

	public static Application instance() {
		if (INSTANCE == null) {
			INSTANCE = new Application();
		}
		return INSTANCE;
	}

	private Application() {
		// Do nothing
	}

	public boolean start() {
		try {
			configureLog4j();

			logger.info("Lannister bootstrapping started");

			Session.NEXUS = new Sessions();
			Topic.NEXUS = new Topics(Session.NEXUS);

			mqttServer = new MqttServer();
			mqttServer.start();

			webServer = new WebServer();
			webServer.start("net.anyflow");

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					shutdown();
				}
			});

			logger.info("Lannister bootstrapping completed");
			logger.info("version        : {}", Settings.SELF.version());
			logger.info("build time     : {}", Settings.SELF.buildTime());
			logger.info("commit ID      : {}", Settings.SELF.commitId());
			logger.info("commit ID desc : {}", Settings.SELF.commitIdDescribe());
			return true;
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
	}

	public void shutdown() {
		logger.info("Lannister shutting down...");

		try {
			webServer.shutdown();
			mqttServer.shutdown();
			Hazelcast.SELF.shutdown();
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		Application.INSTANCE = null;

		logger.info("Lannister shutdowned gracefully");
	}

	public static void main(String[] args) {
		Thread.currentThread().setName("main thread");

		if (!instance().start()) {
			System.exit(-1);
		}
	}

	public static void configureLog4j() {
		org.apache.log4j.xml.DOMConfigurator.configure(Application.class.getClassLoader().getResource("log4j.xml"));
	}
}
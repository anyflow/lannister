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

import net.anyflow.lannister.http.WebServer;
import net.anyflow.lannister.server.MqttServer;
import net.anyflow.lannister.topic.Topic;

public class Application {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Application.class);

	public static final Application INSTANCE = new Application();

	private MqttServer mqttServer;
	private WebServer webServer;

	private Application() {
		// Do nothing
	}

	public boolean start() {
		try {
			configureLog4j();

			logger.info("Lannister bootstrapping started");

			Topic.NEXUS.get(""); // TODO Just for initializing Topic.NEXUS. If
									// absent, NoClassDefFound Error occur in
									// Statistics.. I don't know why

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
			logger.info("build version  : {}", Settings.INSTANCE.version());
			logger.info("build time     : {}", Settings.INSTANCE.buildTime());
			logger.info("commit ID      : {}", Settings.INSTANCE.commitId());
			logger.info("commit ID desc : {}", Settings.INSTANCE.commitIdDescribe());
			logger.info("commit message : {}", Settings.INSTANCE.commitMessage());

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
			Hazelcast.INSTANCE.shutdown();
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		logger.info("Lannister shutdowned gracefully");
	}

	public static void main(String[] args) {
		Thread.currentThread().setName("main thread");

		if (!INSTANCE.start()) {
			System.exit(-1);
		}
	}

	public static void configureLog4j() {
		org.apache.log4j.xml.DOMConfigurator.configure(Application.class.getClassLoader().getResource("log4j.xml"));
	}
}
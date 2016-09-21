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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import net.anyflow.lannister.httphandler.IndexTest;
import net.anyflow.lannister.httphandler.SessionsTest;
import net.anyflow.lannister.httphandler.TopicsTest;
import net.anyflow.lannister.packetreceiver.ConnectReceiverTest;
import net.anyflow.lannister.server.MqttServerTest;
import net.anyflow.lannister.server.SessionExpiratorTest;
import net.anyflow.lannister.session.SessionTest;
import net.anyflow.lannister.session.WillTest;
import net.anyflow.lannister.topic.TopicMatcherTest;
import net.anyflow.lannister.topic.TopicTest;

@RunWith(Suite.class)
@SuiteClasses({ NettyUtilTest.class, IndexTest.class, SessionsTest.class, ConnectReceiverTest.class,
		TopicMatcherTest.class, TopicsTest.class, MqttServerTest.class, TopicTest.class, SessionTest.class,
		WillTest.class, SessionExpiratorTest.class })
public class TestSuite {

	private static boolean SETUP_CALLED = false;

	@BeforeClass
	public static void setUp() {
		if (SETUP_CALLED) { return; }

		Application.main(null);
		SETUP_CALLED = true;
	}

	@AfterClass
	public static void tearDown() {
		Application.INSTANCE.shutdown();
	}
}
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

import net.anyflow.lannister.admin.command.SessionsFilterTest;
import net.anyflow.lannister.admin.command.TopicsFilterTest;
import net.anyflow.lannister.packetreceiver.ConnectReceiverTest;
import net.anyflow.lannister.topic.TopicMatcherTest;

@RunWith(Suite.class)
@SuiteClasses({ SessionsFilterTest.class, ConnectReceiverTest.class, TopicMatcherTest.class, TopicsFilterTest.class,
		CodecTest.class, MqttServerTest.class })
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
		Application.instance().shutdown();
	}
}
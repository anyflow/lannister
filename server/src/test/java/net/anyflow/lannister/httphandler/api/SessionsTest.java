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

package net.anyflow.lannister.httphandler.api;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jayway.jsonpath.JsonPath;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.util.CharsetUtil;
import net.anyflow.lannister.Settings;
import net.anyflow.lannister.TestSuite;
import net.anyflow.lannister.TestUtil;
import net.anyflow.lannister.client.MqttClient;
import net.anyflow.lannister.http.HttpClient;
import net.anyflow.lannister.http.HttpResponse;
import net.anyflow.lannister.message.ConnectOptions;

public class SessionsTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestSuite.setUp();
	}

	@Test
	public void testLive() throws Exception {
		ConnectOptions options = new ConnectOptions();
		options.clientId(TestUtil.newClientId());

		MqttClient client = new MqttClient("mqtt://localhost:" + Settings.INSTANCE.mqttPort());
		MqttConnectReturnCode ret = client.connectOptions(options).connect();

		Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, ret);

		Assert.assertTrue(client.isConnected());

		HttpClient httpClient = new HttpClient(
				"http://localhost:" + Settings.INSTANCE.httpPort() + "/api/sessions?filter=live");
		HttpResponse res = httpClient.get();

		Assert.assertEquals(HttpResponseStatus.OK, res.status());
		Assert.assertEquals(new Integer(1), JsonPath.read(res.content().toString(CharsetUtil.UTF_8), "$.length()"));

		client.disconnect(true);

		Assert.assertFalse(client.isConnected());
	}

	@Test
	public void testAll() throws Exception {
		ConnectOptions options = new ConnectOptions();
		options.clientId(TestUtil.newClientId());

		MqttClient client = new MqttClient("mqtt://localhost:" + Settings.INSTANCE.mqttPort());
		MqttConnectReturnCode ret = client.connectOptions(options).connect();

		Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, ret);

		Assert.assertTrue(client.isConnected());

		options = new ConnectOptions();
		options.clientId(TestUtil.newClientId());
		options.cleanSession(false);

		MqttClient client2 = new MqttClient("mqtt://localhost:" + Settings.INSTANCE.mqttPort());
		ret = client2.connectOptions(options).connect();

		Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, ret);

		Assert.assertTrue(client2.isConnected());

		client2.disconnect(true);

		Assert.assertFalse(client2.isConnected());

		HttpClient httpClient = new HttpClient(
				"http://localhost:" + Settings.INSTANCE.httpPort() + "/api/sessions?filter=all");
		HttpResponse res = httpClient.get();

		Assert.assertEquals(HttpResponseStatus.OK, res.status());
		Assert.assertEquals(new Integer(2), JsonPath.read(res.content().toString(CharsetUtil.UTF_8), "$.length()"));

		client.disconnect(true);

		Assert.assertFalse(client.isConnected());
	}
}
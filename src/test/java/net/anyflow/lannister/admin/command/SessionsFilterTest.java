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

package net.anyflow.lannister.admin.command;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import net.anyflow.lannister.TestSuite;
import net.anyflow.menton.http.HttpClient;
import net.anyflow.menton.http.HttpResponse;

public class SessionsFilterTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		TestSuite.setUp();
	}

	@Test
	public void testLive() throws Exception {
		HttpClient client = new HttpClient("http://localhost:8090/sessions?filter=live");
		HttpResponse res = client.get();

		Assert.assertEquals(HttpResponseStatus.OK, res.status());
		Assert.assertTrue(res.content().toString(CharsetUtil.UTF_8).startsWith("{"));
		Assert.assertTrue(res.content().toString(CharsetUtil.UTF_8).endsWith("}"));
	}

	@Test
	public void testAll() throws Exception {
		HttpClient client = new HttpClient("http://localhost:8090/sessions?filter=all");
		HttpResponse res = client.get();

		Assert.assertEquals(HttpResponseStatus.OK, res.status());
		Assert.assertTrue(res.content().toString(CharsetUtil.UTF_8).startsWith("{"));
		Assert.assertTrue(res.content().toString(CharsetUtil.UTF_8).endsWith("}"));
	}
}

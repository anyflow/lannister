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

package net.anyflow.lannister.topic;

import org.hamcrest.core.IsEqual;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.anyflow.lannister.Application;

public class TopicMatcherTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Application.configureLog4j();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// Do nothing
	}

	@Test
	public void testMatch() throws Exception {
		Assert.assertThat(TopicMatcher.match("test/1/2", "test/1/2"), IsEqual.equalTo(true));

		Assert.assertThat(TopicMatcher.match("test+", "test_1"), IsEqual.equalTo(false));
		Assert.assertThat(TopicMatcher.match("test#", "test_1"), IsEqual.equalTo(false));

		Assert.assertThat(TopicMatcher.match("+test+", "1_test"), IsEqual.equalTo(false));
		Assert.assertThat(TopicMatcher.match("#test", "1_test"), IsEqual.equalTo(false));

		Assert.assertThat(TopicMatcher.match("#", ""), IsEqual.equalTo(false));
		Assert.assertThat(TopicMatcher.match("#", "/"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("#", "test"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("#", "test/1"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("+", "test"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("+", "test/1"), IsEqual.equalTo(false));
		Assert.assertThat(TopicMatcher.match("test/+", "test/1"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("test/#", "test/1"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("test/#", "test/1/2"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("test/+/", "test/1"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("test/+/", "test/1/"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("test/+/2", "test/1/2"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("test/#/2", "test/1/2"), IsEqual.equalTo(false));

		Assert.assertThat(TopicMatcher.match("/#", "/test"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("/test/+", "/test/1"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("/test/#", "/test/1"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("/test/#", "/test/1/2"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("/test/+/", "/test/1"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("/test/+/", "/test/1/"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("/test/+/2", "/test/1/2"), IsEqual.equalTo(true));
		Assert.assertThat(TopicMatcher.match("/test/#/2", "/test/1/2"), IsEqual.equalTo(false));

		Assert.assertThat(TopicMatcher.match("/test/+/2", "/test/1/3"), IsEqual.equalTo(false));
	}
}
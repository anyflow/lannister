package net.anyflow.lannister.topic;

import org.hamcrest.core.IsEqual;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TopicMatcherTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Do nothing
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